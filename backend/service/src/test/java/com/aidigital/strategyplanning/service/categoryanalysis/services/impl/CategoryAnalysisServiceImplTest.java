package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.domain.categoryanalysis.entities.CategoryAnalysisEntity;
import com.aidigital.strategyplanning.domain.categoryanalysis.repositories.CategoryAnalysisRepository;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateCategoryAnalysisCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateInDepthDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.models.RecordOnePagerCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleDeckService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDeckService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDraftService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardDraftService;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.time.CurrentTimeImpl;
import com.aidigital.strategyplanning.service.mappers.categoryanalysis.CategoryAnalysisMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
 * Unit tests for the category analysis aggregate service.
 *
 * <p>This aggregate carries two write paths: the manual builder saves without touching Google at
 * all, while the standard deck path publishes first and saves the deck URL. The connection check
 * exists so the UI can explain what is missing before the user spends time drafting.
 */
class CategoryAnalysisServiceImplTest {

	@Test
	void shouldSaveAManualAnalysisWithoutTouchingGoogleTest() {
		// Given: a hand-written analysis, which has no deck
		CategoryAnalysisRepository repository = mock(CategoryAnalysisRepository.class);
		GoogleDeckService deckService = mock(GoogleDeckService.class);
		StandardDraftService draftService = mock(StandardDraftService.class);
		CreateCategoryAnalysisCommand command = new CreateCategoryAnalysisCommand(
				"Pet Care 2026", "Pet Care", "Growing steadily.", "Acme, Globex",
				"Premiumisation", "Subscription refills", "Launch a bundle",
				"market.pdf", "user_1");
		when(repository.save(org.mockito.ArgumentMatchers.<CategoryAnalysisEntity>any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When: it is created
		CategoryAnalysisRecord record = new CategoryAnalysisServiceImpl(repository,
				Mappers.getMapper(CategoryAnalysisMapper.class), draftService, deckService,
				mock(InDepthDraftService.class), mock(InDepthDeckService.class), new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl()).create(command);

		// Then: the row is saved as submitted with no deck, and no Google call was made
		assertThat(record.title()).isEqualTo("Pet Care 2026");
		assertThat(record.status()).isEqualTo("SUBMITTED");
		assertThat(record.slidesUrl()).isNull();
		assertThat(record.createdAt()).isNotNull();
		verifyNoInteractions(deckService, draftService);
	}

	@Test
	void shouldPublishTheStandardDeckWithATitleBuiltFromCategoryAndClientTest() {
		// Given: a reviewed standard deck with its field values
		CategoryAnalysisRepository repository = mock(CategoryAnalysisRepository.class);
		GoogleDeckService deckService = mock(GoogleDeckService.class);
		CreateStandardDeckCommand command = new CreateStandardDeckCommand("Pet Care", "Acme",
				List.of(new StandardFieldValue("headline", "Pets are premium now")), "user_1");
		String expectedTitle = "Category Analysis — Pet Care — Acme";
		when(deckService.createDeck("user_1", expectedTitle, "Pet Care",
				Map.of("headline", "Pets are premium now"), "https://app.example.com"))
				.thenReturn("https://docs.google.com/presentation/d/deck_1/edit");
		when(repository.save(org.mockito.ArgumentMatchers.<CategoryAnalysisEntity>any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When: the deck is created
		CategoryAnalysisRecord record = new CategoryAnalysisServiceImpl(repository,
				Mappers.getMapper(CategoryAnalysisMapper.class), mock(StandardDraftService.class),
				deckService, mock(InDepthDraftService.class), mock(InDepthDeckService.class),
				new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl())
				.createStandardDeck(command, "https://app.example.com");

		// Then: the deck title the user sees in Drive names the category and the client, and the
		// row records that this is a standard-template deck
		assertThat(record.title()).isEqualTo(expectedTitle);
		assertThat(record.slidesUrl())
				.isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(record.templateKind()).isEqualTo("STANDARD");
		assertThat(record.status()).isEqualTo("SUBMITTED");
		verify(deckService).createDeck("user_1", expectedTitle, "Pet Care",
				Map.of("headline", "Pets are premium now"), "https://app.example.com");
	}

	@Test
	void shouldNotSaveARowWhenTheStandardDeckCouldNotBePublishedTest() {
		// Given: a Google flow that rejects the deck creation
		CategoryAnalysisRepository repository = mock(CategoryAnalysisRepository.class);
		GoogleDeckService deckService = mock(GoogleDeckService.class);
		CreateStandardDeckCommand command = new CreateStandardDeckCommand("Pet Care", "Acme",
				List.of(new StandardFieldValue("headline", "Pets are premium now")), "user_1");
		when(deckService.createDeck("user_1", "Category Analysis — Pet Care — Acme", "Pet Care",
				Map.of("headline", "Pets are premium now"), "https://app.example.com"))
				.thenThrow(new IllegalStateException("Google refused"));

		// When-Then: nothing is persisted, so no analysis links to a deck that was never created
		assertThatThrownBy(() -> new CategoryAnalysisServiceImpl(repository,
				Mappers.getMapper(CategoryAnalysisMapper.class), mock(StandardDraftService.class),
				deckService, mock(InDepthDraftService.class), mock(InDepthDeckService.class),
				new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl())
				.createStandardDeck(command, "https://app.example.com"))
				.isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(repository);
	}

	@Test
	void shouldReportBothConnectionsAsReadyTest() {
		// Given: AI configured and Google granted with the scopes a deck needs
		GoogleDeckService deckService = mock(GoogleDeckService.class);
		StandardDraftService draftService = mock(StandardDraftService.class);
		when(draftService.isConnected()).thenReturn(true);
		when(deckService.checkGoogleConnection("user_1"))
				.thenReturn(GoogleConnectionStatus.CONNECTED);

		// When: the connections are checked
		StandardConnections connections = new CategoryAnalysisServiceImpl(
				mock(CategoryAnalysisRepository.class),
				Mappers.getMapper(CategoryAnalysisMapper.class), draftService, deckService,
				mock(InDepthDraftService.class), mock(InDepthDeckService.class), new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl()).getStandardConnections("user_1");

		// Then: the UI can let the user start
		assertThat(connections.aiConnected()).isTrue();
		assertThat(connections.googleConnected()).isTrue();
		assertThat(connections.googleStatus()).isEqualTo(GoogleConnectionStatus.CONNECTED);
	}

	@Test
	void shouldReportGoogleAsNotUsableWhenScopesAreMissingTest() {
		// Given: Google connected but without the Drive and Slides scopes
		GoogleDeckService deckService = mock(GoogleDeckService.class);
		StandardDraftService draftService = mock(StandardDraftService.class);
		when(draftService.isConnected()).thenReturn(false);
		when(deckService.checkGoogleConnection("user_1"))
				.thenReturn(GoogleConnectionStatus.MISSING_SCOPES);

		// When: the connections are checked
		StandardConnections connections = new CategoryAnalysisServiceImpl(
				mock(CategoryAnalysisRepository.class),
				Mappers.getMapper(CategoryAnalysisMapper.class), draftService, deckService,
				mock(InDepthDraftService.class), mock(InDepthDeckService.class), new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl()).getStandardConnections("user_1");

		// Then: "connected" is false even though a grant exists, and the precise reason is
		// carried through so the UI can say "reconnect" rather than "sign in"
		assertThat(connections.aiConnected()).isFalse();
		assertThat(connections.googleConnected()).isFalse();
		assertThat(connections.googleStatus()).isEqualTo(GoogleConnectionStatus.MISSING_SCOPES);
	}

	@Test
	void shouldListOnlyTheCallersOwnAnalysesNewestFirstTest() {
		// Given: the caller's saved analyses as the repository orders them
		CategoryAnalysisRepository repository = mock(CategoryAnalysisRepository.class);
		CategoryAnalysisEntity newer = new CategoryAnalysisEntity();
		newer.setTitle("Newer");
		CategoryAnalysisEntity older = new CategoryAnalysisEntity();
		older.setTitle("Older");
		when(repository.findByCreatedByOrderByCreatedAtDesc("user_1"))
				.thenReturn(List.of(newer, older));

		// When: the list is requested
		List<CategoryAnalysisRecord> records = new CategoryAnalysisServiceImpl(repository,
				Mappers.getMapper(CategoryAnalysisMapper.class), mock(StandardDraftService.class),
				mock(GoogleDeckService.class), mock(InDepthDraftService.class),
				mock(InDepthDeckService.class), new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl()).listByUser("user_1");

		// Then: the query is scoped to the caller and the order is preserved
		assertThat(records).extracting(CategoryAnalysisRecord::title)
				.containsExactly("Newer", "Older");
		verify(repository).findByCreatedByOrderByCreatedAtDesc("user_1");
	}

	@Test
	void shouldReturnAnAnalysisOwnedByTheCallerTest() {
		// Given: an analysis that belongs to the caller
		CategoryAnalysisRepository repository = mock(CategoryAnalysisRepository.class);
		CategoryAnalysisEntity entity = new CategoryAnalysisEntity();
		entity.setId(5L);
		entity.setTitle("Pet Care 2026");
		when(repository.findByIdAndCreatedBy(5L, "user_1")).thenReturn(Optional.of(entity));

		// When: it is fetched by id
		CategoryAnalysisRecord record = new CategoryAnalysisServiceImpl(repository,
				Mappers.getMapper(CategoryAnalysisMapper.class), mock(StandardDraftService.class),
				mock(GoogleDeckService.class), mock(InDepthDraftService.class),
				mock(InDepthDeckService.class), new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl()).getById(5L, "user_1");

		// Then: the caller gets their own row
		assertThat(record.id()).isEqualTo(5L);
		assertThat(record.title()).isEqualTo("Pet Care 2026");
	}

	@Test
	void shouldNotRevealAnotherUsersAnalysisTest() {
		// Given: an id that exists but belongs to someone else
		CategoryAnalysisRepository repository = mock(CategoryAnalysisRepository.class);
		when(repository.findByIdAndCreatedBy(5L, "user_2")).thenReturn(Optional.empty());

		// When-Then: "not found" rather than "forbidden", which would confirm the row exists
		assertThatThrownBy(() -> new CategoryAnalysisServiceImpl(repository,
				Mappers.getMapper(CategoryAnalysisMapper.class), mock(StandardDraftService.class),
				mock(GoogleDeckService.class), mock(InDepthDraftService.class),
				mock(InDepthDeckService.class), new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl()).getById(5L, "user_2"))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getCode())
						.isEqualTo("C001"));
	}

	@Test
	void shouldServeTheInDepthSlideCatalogFromTheRegistryTest() {
		// Given: the aggregate wired with the real registry
		CategoryAnalysisServiceImpl service = new CategoryAnalysisServiceImpl(
				mock(CategoryAnalysisRepository.class), Mappers.getMapper(CategoryAnalysisMapper.class),
				mock(StandardDraftService.class), mock(GoogleDeckService.class), mock(InDepthDraftService.class),
				mock(InDepthDeckService.class), new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl());

		// When-Then: all 24 slides are listed in deck order
		assertThat(service.listInDepthSlides()).hasSize(24);
		assertThat(service.listInDepthSlides().get(0).slideNumber()).isEqualTo(1);
	}

	@Test
	void shouldDelegateInDepthDraftingAndRedraftingTest() {
		// Given: a drafting service that answers both calls
		InDepthDraftService draftService = mock(InDepthDraftService.class);
		InDepthDraft draft = new InDepthDraft(List.of(), null);
		when(draftService.draftInDepth("Pet Care", "Acme", "notes", "https://acme.com", List.of(2)))
				.thenReturn(draft);
		List<InDepthFieldValue> current = List.of(new InDepthFieldValue("trend_headline", "X"));
		when(draftService.redraftSlide("Pet Care", "Acme", "notes", 2, "punchier", current)).thenReturn(draft);
		CategoryAnalysisServiceImpl service = new CategoryAnalysisServiceImpl(
				mock(CategoryAnalysisRepository.class), Mappers.getMapper(CategoryAnalysisMapper.class),
				mock(StandardDraftService.class), mock(GoogleDeckService.class), draftService, mock(InDepthDeckService.class),
				new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl());

		// When-Then: the aggregate passes the brief through unchanged
		assertThat(service.draftInDepth("Pet Care", "Acme", "notes", "https://acme.com", List.of(2)))
				.isSameAs(draft);
		assertThat(service.redraftInDepthSlide("Pet Care", "Acme", "notes", 2, "punchier", current))
				.isSameAs(draft);
	}

	@Test
	void shouldPublishTheInDepthDeckAndRecordItsTemplateKindTest() {
		// Given: a reviewed In-Depth deck with two checked slides
		CategoryAnalysisRepository repository = mock(CategoryAnalysisRepository.class);
		InDepthDeckService deckService = mock(InDepthDeckService.class);
		CreateInDepthDeckCommand command = new CreateInDepthDeckCommand("Pet Care", "Acme", List.of(1, 2),
				List.of(new InDepthFieldValue("trend_headline", "PETS ARE PREMIUM")), "user_1");
		String expectedTitle = "Category Analysis — Pet Care — Acme";
		when(deckService.createInDepthDeck("user_1", expectedTitle, List.of(1, 2),
				Map.of("trend_headline", "PETS ARE PREMIUM"), "https://app.example.com"))
				.thenReturn("https://docs.google.com/presentation/d/deck_2/edit");
		when(repository.save(org.mockito.ArgumentMatchers.<CategoryAnalysisEntity>any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When: the deck is created
		CategoryAnalysisRecord record = new CategoryAnalysisServiceImpl(repository,
				Mappers.getMapper(CategoryAnalysisMapper.class), mock(StandardDraftService.class),
				mock(GoogleDeckService.class), mock(InDepthDraftService.class), deckService,
				new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl())
				.createInDepthDeck(command, "https://app.example.com");

		// Then: the row records the In-Depth template and the deck URL
		assertThat(record.title()).isEqualTo(expectedTitle);
		assertThat(record.category()).isEqualTo("Pet Care");
		assertThat(record.slidesUrl()).isEqualTo("https://docs.google.com/presentation/d/deck_2/edit");
		assertThat(record.templateKind()).isEqualTo("IN_DEPTH");
		assertThat(record.status()).isEqualTo("SUBMITTED");
		assertThat(record.createdBy()).isEqualTo("user_1");
		assertThat(record.createdAt()).isNotNull();
	}

	@Test
	void shouldNotSaveARowWhenTheInDepthDeckCouldNotBePublishedTest() {
		// Given: a Google flow that rejects the deck creation
		CategoryAnalysisRepository repository = mock(CategoryAnalysisRepository.class);
		InDepthDeckService deckService = mock(InDepthDeckService.class);
		CreateInDepthDeckCommand command = new CreateInDepthDeckCommand("Pet Care", "Acme", List.of(2),
				List.of(), "user_1");
		when(deckService.createInDepthDeck("user_1", "Category Analysis — Pet Care — Acme", List.of(2),
				Map.of(), "https://app.example.com"))
				.thenThrow(new IllegalStateException("Google refused"));

		// When-Then: nothing is persisted
		assertThatThrownBy(() -> new CategoryAnalysisServiceImpl(repository,
				Mappers.getMapper(CategoryAnalysisMapper.class), mock(StandardDraftService.class),
				mock(GoogleDeckService.class), mock(InDepthDraftService.class), deckService,
				new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl())
				.createInDepthDeck(command, "https://app.example.com"))
				.isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(repository);
	}

	@Test
	void shouldRecordAOnePagerAsACategoryAnalysisRowWithoutTouchingGoogleTest() {
		// Given: a page that already exists in the caller's Drive
		CategoryAnalysisRepository repository = mock(CategoryAnalysisRepository.class);
		GoogleDeckService deckService = mock(GoogleDeckService.class);
		InDepthDeckService inDepthDeckService = mock(InDepthDeckService.class);
		RecordOnePagerCommand command = new RecordOnePagerCommand("One Page Generator — Pet Care — Acme",
				"Pet Care", "https://docs.google.com/presentation/d/page_1/edit", "user_1");
		when(repository.save(org.mockito.ArgumentMatchers.<CategoryAnalysisEntity>any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When: it is recorded
		CategoryAnalysisRecord record = new CategoryAnalysisServiceImpl(repository,
				Mappers.getMapper(CategoryAnalysisMapper.class), mock(StandardDraftService.class),
				deckService, mock(InDepthDraftService.class), inDepthDeckService,
				new InDepthTemplateTokenRegistryImpl(), new CurrentTimeImpl()).recordOnePager(command);

		// Then: the row carries the ONE_PAGER kind, the link, and the creator, with empty narrative
		// columns, and no Google call was made
		ArgumentCaptor<CategoryAnalysisEntity> saved = ArgumentCaptor.forClass(CategoryAnalysisEntity.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getTemplateKind()).isEqualTo("ONE_PAGER");
		assertThat(saved.getValue().getMarketOverview()).isNull();
		assertThat(record.title()).isEqualTo("One Page Generator — Pet Care — Acme");
		assertThat(record.category()).isEqualTo("Pet Care");
		assertThat(record.slidesUrl()).isEqualTo("https://docs.google.com/presentation/d/page_1/edit");
		assertThat(record.templateKind()).isEqualTo("ONE_PAGER");
		assertThat(record.status()).isEqualTo("SUBMITTED");
		assertThat(record.createdBy()).isEqualTo("user_1");
		assertThat(record.createdAt()).isNotNull();
		verifyNoInteractions(deckService, inDepthDeckService);
	}
}
