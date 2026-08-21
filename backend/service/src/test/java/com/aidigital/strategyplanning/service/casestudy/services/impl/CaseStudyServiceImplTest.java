package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.domain.casestudy.entities.CaseStudyEntity;
import com.aidigital.strategyplanning.domain.casestudy.repositories.CaseStudyRepository;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDeckService;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDraftService;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.time.CurrentTimeImpl;
import com.aidigital.strategyplanning.service.mappers.casestudy.CaseStudyMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the case study aggregate service.
 *
 * <p>The deck is published before the row is saved, on purpose: a saved case study without a
 * working deck link would be worse than a failed request the user can retry.
 */
class CaseStudyServiceImplTest {

	/**
	 * Builds a create command with the given title.
	 *
	 * @param title case study title
	 * @return validated create command
	 */
	private CreateCaseStudyCommand command(String title) {
		return new CreateCaseStudyCommand(title, "Acme", "Retail",
				"Sales were declining.", "We rebuilt the funnel.", "Revenue grew.",
				"40% cost reduction", "6 months", null, null, "user_1");
	}

	@Test
	void shouldPublishTheDeckThenSaveTheRowTest() {
		// Given: a reviewed case study and a Google flow that publishes a deck
		CaseStudyRepository repository = mock(CaseStudyRepository.class);
		CaseStudyDraftService draftService = mock(CaseStudyDraftService.class);
		CaseStudyDeckService deckService = mock(CaseStudyDeckService.class);
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setTemplateUrl("https://docs.google.com/presentation/d/template/edit");
		CreateCaseStudyCommand command = command("Acme Turnaround");
		Map<String, String> tokens = Map.of("client_vertical", "Retail");
		when(draftService.buildTemplateTokenValues(command)).thenReturn(tokens);
		when(deckService.createDeck("user_1", "Acme Turnaround", tokens))
				.thenReturn("https://docs.google.com/presentation/d/deck_1/edit");
		when(repository.save(org.mockito.ArgumentMatchers.<CaseStudyEntity>any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When: the case study is created
		CaseStudyRecord record = new CaseStudyServiceImpl(repository,
				Mappers.getMapper(CaseStudyMapper.class), properties, draftService, deckService,
				new CurrentTimeImpl()).create(command);

		// Then: the published deck URL and the configured template are both persisted
		assertThat(record.slidesUrl())
				.isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(record.templateUrl())
				.isEqualTo("https://docs.google.com/presentation/d/template/edit");
		assertThat(record.status()).isEqualTo("GENERATED");
		assertThat(record.createdAt()).isNotNull();
	}

	@Test
	void shouldNotSaveARowWhenTheDeckCouldNotBePublishedTest() {
		// Given: a Google flow that rejects the deck creation
		CaseStudyRepository repository = mock(CaseStudyRepository.class);
		CaseStudyDraftService draftService = mock(CaseStudyDraftService.class);
		CaseStudyDeckService deckService = mock(CaseStudyDeckService.class);
		CreateCaseStudyCommand command = command("Acme Turnaround");
		Map<String, String> tokens = Map.of("client_vertical", "Retail");
		when(draftService.buildTemplateTokenValues(command)).thenReturn(tokens);
		when(deckService.createDeck("user_1", "Acme Turnaround", tokens))
				.thenThrow(new IllegalStateException("Google refused"));

		// When-Then: nothing is persisted, so the user does not end up with a case study whose
		// deck link goes nowhere
		assertThatThrownBy(() -> new CaseStudyServiceImpl(repository,
				Mappers.getMapper(CaseStudyMapper.class), new CaseStudyProperties(), draftService,
				deckService, new CurrentTimeImpl()).create(command))
				.isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(repository);
	}

	@Test
	void shouldListOnlyTheCallersOwnCaseStudiesNewestFirstTest() {
		// Given: the caller's saved case studies as the repository orders them
		CaseStudyRepository repository = mock(CaseStudyRepository.class);
		CaseStudyEntity newer = new CaseStudyEntity();
		newer.setTitle("Newer");
		CaseStudyEntity older = new CaseStudyEntity();
		older.setTitle("Older");
		when(repository.findByCreatedByOrderByCreatedAtDesc("user_1"))
				.thenReturn(List.of(newer, older));

		// When: the list is requested
		List<CaseStudyRecord> records = new CaseStudyServiceImpl(repository,
				Mappers.getMapper(CaseStudyMapper.class), new CaseStudyProperties(),
				mock(CaseStudyDraftService.class), mock(CaseStudyDeckService.class),
				new CurrentTimeImpl()).listByUser("user_1");

		// Then: the query is scoped to the caller and the order is preserved
		assertThat(records).extracting(CaseStudyRecord::title).containsExactly("Newer", "Older");
		verify(repository).findByCreatedByOrderByCreatedAtDesc("user_1");
	}

	@Test
	void shouldReturnACaseStudyOwnedByTheCallerTest() {
		// Given: a case study that belongs to the caller
		CaseStudyRepository repository = mock(CaseStudyRepository.class);
		CaseStudyEntity entity = new CaseStudyEntity();
		entity.setId(7L);
		entity.setTitle("Acme Turnaround");
		when(repository.findByIdAndCreatedBy(7L, "user_1")).thenReturn(Optional.of(entity));

		// When: it is fetched by id
		CaseStudyRecord record = new CaseStudyServiceImpl(repository,
				Mappers.getMapper(CaseStudyMapper.class), new CaseStudyProperties(),
				mock(CaseStudyDraftService.class), mock(CaseStudyDeckService.class),
				new CurrentTimeImpl()).getById(7L, "user_1");

		// Then: the caller gets their own row
		assertThat(record.id()).isEqualTo(7L);
		assertThat(record.title()).isEqualTo("Acme Turnaround");
	}

	@Test
	void shouldNotRevealAnotherUsersCaseStudyTest() {
		// Given: an id that exists but belongs to someone else, so the scoped lookup misses
		CaseStudyRepository repository = mock(CaseStudyRepository.class);
		when(repository.findByIdAndCreatedBy(7L, "user_2")).thenReturn(Optional.empty());

		// When-Then: the caller gets "not found" rather than "forbidden", which would confirm
		// that the row exists
		assertThatThrownBy(() -> new CaseStudyServiceImpl(repository,
				Mappers.getMapper(CaseStudyMapper.class), new CaseStudyProperties(),
				mock(CaseStudyDraftService.class), mock(CaseStudyDeckService.class),
				new CurrentTimeImpl()).getById(7L, "user_2"))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getCode())
						.isEqualTo("C001"));
	}
}
