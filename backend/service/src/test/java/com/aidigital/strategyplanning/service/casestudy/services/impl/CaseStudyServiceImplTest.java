package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.domain.casestudy.entities.CaseStudyEntity;
import com.aidigital.strategyplanning.domain.casestudy.repositories.CaseStudyRepository;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.models.CreateOnePageCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudySummary;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconChoice;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDeckService;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDraftService;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageCaseStudyContentAssembler;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageCaseStudyDeckService;
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
import static org.mockito.ArgumentMatchers.anyList;
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
				mock(OnePageCaseStudyDeckService.class), mock(OnePageCaseStudyContentAssembler.class),
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
				deckService, mock(OnePageCaseStudyDeckService.class),
				mock(OnePageCaseStudyContentAssembler.class), new CurrentTimeImpl()).create(command))
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
				mock(OnePageCaseStudyDeckService.class), mock(OnePageCaseStudyContentAssembler.class),
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
				mock(OnePageCaseStudyDeckService.class), mock(OnePageCaseStudyContentAssembler.class),
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
				mock(OnePageCaseStudyDeckService.class), mock(OnePageCaseStudyContentAssembler.class),
				new CurrentTimeImpl()).getById(7L, "user_2"))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getCode())
						.isEqualTo("C001"));
	}

	@Test
	void shouldPublishTheOnePageDeckThenSaveTheSummaryRowTest() {
		// Given: reviewed one-page content, an assembler that derives tokens and a summary, and a deck flow
		CaseStudyRepository repository = mock(CaseStudyRepository.class);
		OnePageCaseStudyDeckService onePageDeckService = mock(OnePageCaseStudyDeckService.class);
		OnePageCaseStudyContentAssembler assembler = mock(OnePageCaseStudyContentAssembler.class);
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setOnePageTemplatePresentationId("one_page_template");
		List<OnePageFieldValue> fields = List.of(new OnePageFieldValue("title_line_1", "Scaling"));
		List<OnePageIconChoice> icons = List.of(new OnePageIconChoice("plan_icon", "compass"));
		CreateOnePageCaseStudyCommand command = new CreateOnePageCaseStudyCommand(fields, icons, "report.pdf",
				"user_1");
		Map<String, String> tokens = Map.of("title_line_1", "SCALING");
		Map<String, String> iconUrls = Map.of("plan_icon", "https://app/case-study-icons/compass.png");
		OnePageCaseStudySummary summary = new OnePageCaseStudySummary("SCALING", "A retailer", "RETAIL",
				"CPA rose.", "Reallocated budget.", "Conversions grew.", "-68%", "April – August 2026");
		when(assembler.buildTokenValues(fields)).thenReturn(tokens);
		when(assembler.summarize(tokens)).thenReturn(summary);
		when(assembler.buildIconUrls(icons, "https://app.example.com")).thenReturn(iconUrls);
		when(onePageDeckService.createDeck("user_1", "Case Study — SCALING", tokens, iconUrls))
				.thenReturn("https://docs.google.com/presentation/d/deck_1/edit");
		when(repository.save(org.mockito.ArgumentMatchers.<CaseStudyEntity>any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When: the one-page case study is created
		CaseStudyRecord record = new CaseStudyServiceImpl(repository, Mappers.getMapper(CaseStudyMapper.class),
				properties, mock(CaseStudyDraftService.class), mock(CaseStudyDeckService.class), onePageDeckService,
				assembler, new CurrentTimeImpl()).createOnePage(command, "https://app.example.com");

		// Then: the summary, the deck link, and the one-page template URL are persisted together
		assertThat(record.title()).isEqualTo("SCALING");
		assertThat(record.clientName()).isEqualTo("A retailer");
		assertThat(record.industry()).isEqualTo("RETAIL");
		assertThat(record.solution()).isEqualTo("Reallocated budget.");
		assertThat(record.keyMetrics()).isEqualTo("-68%");
		assertThat(record.timeline()).isEqualTo("April – August 2026");
		assertThat(record.sourceFileName()).isEqualTo("report.pdf");
		assertThat(record.slidesUrl()).isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(record.templateUrl()).isEqualTo("https://docs.google.com/presentation/d/one_page_template/edit");
		assertThat(record.status()).isEqualTo("GENERATED");
		assertThat(record.createdBy()).isEqualTo("user_1");
		assertThat(record.testimonial()).isNull();
	}

	@Test
	void shouldNotSaveAOnePageRowWhenTheDeckCouldNotBePublishedTest() {
		// Given: a deck flow that refuses
		CaseStudyRepository repository = mock(CaseStudyRepository.class);
		OnePageCaseStudyDeckService onePageDeckService = mock(OnePageCaseStudyDeckService.class);
		OnePageCaseStudyContentAssembler assembler = mock(OnePageCaseStudyContentAssembler.class);
		CreateOnePageCaseStudyCommand command = new CreateOnePageCaseStudyCommand(List.of(), List.of(), null,
				"user_1");
		Map<String, String> tokens = Map.of();
		when(assembler.buildTokenValues(anyList())).thenReturn(tokens);
		when(assembler.summarize(tokens)).thenReturn(new OnePageCaseStudySummary("One-Page Case Study", null, null,
				null, null, null, null, null));
		when(assembler.buildIconUrls(anyList(), org.mockito.ArgumentMatchers.eq("https://app.example.com")))
				.thenReturn(Map.of());
		when(onePageDeckService.createDeck("user_1", "Case Study — One-Page Case Study", tokens, Map.of()))
				.thenThrow(new IllegalStateException("Google refused"));

		// When-Then: nothing is persisted
		assertThatThrownBy(() -> new CaseStudyServiceImpl(repository, Mappers.getMapper(CaseStudyMapper.class),
				new CaseStudyProperties(), mock(CaseStudyDraftService.class), mock(CaseStudyDeckService.class),
				onePageDeckService, assembler, new CurrentTimeImpl()).createOnePage(command, "https://app.example.com"))
				.isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(repository);
	}
}
