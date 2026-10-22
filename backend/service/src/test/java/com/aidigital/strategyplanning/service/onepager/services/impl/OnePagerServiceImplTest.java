package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.models.RecordOnePagerCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.categoryanalysis.services.CategoryAnalysisService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleDeckService;
import com.aidigital.strategyplanning.service.onepager.OnePagerFixtures;
import com.aidigital.strategyplanning.service.onepager.models.CreateOnePagerCommand;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDeck;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDeckService;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDraftService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the One Page Generator aggregate service.
 */
class OnePagerServiceImplTest {

	@Test
	void shouldCombineTheAiAndGoogleConnectionStatesTest() {
		// Given: a connected AI engine and a Google grant missing scopes
		OnePagerDraftService draftService = mock(OnePagerDraftService.class);
		GoogleDeckService googleDeckService = mock(GoogleDeckService.class);
		when(draftService.isConnected()).thenReturn(true);
		when(googleDeckService.checkGoogleConnection("user_1")).thenReturn(GoogleConnectionStatus.MISSING_SCOPES);

		// When: connections are checked
		StandardConnections connections = new OnePagerServiceImpl(draftService, mock(OnePagerDeckService.class),
				googleDeckService, mock(CategoryAnalysisService.class)).getConnections("user_1");

		// Then: AI is on, Google is off with the detailed state
		assertThat(connections).isEqualTo(new StandardConnections(true, false, GoogleConnectionStatus.MISSING_SCOPES));
	}

	@Test
	void shouldReportGoogleConnectedOnlyWhenEveryScopeIsGrantedTest() {
		// Given: a full Google grant and no AI key
		OnePagerDraftService draftService = mock(OnePagerDraftService.class);
		GoogleDeckService googleDeckService = mock(GoogleDeckService.class);
		when(googleDeckService.checkGoogleConnection("user_1")).thenReturn(GoogleConnectionStatus.CONNECTED);

		// When: connections are checked
		StandardConnections connections = new OnePagerServiceImpl(draftService, mock(OnePagerDeckService.class),
				googleDeckService, mock(CategoryAnalysisService.class)).getConnections("user_1");

		// Then: Google is on, AI is off
		assertThat(connections.googleConnected()).isTrue();
		assertThat(connections.aiConnected()).isFalse();
	}

	@Test
	void shouldDelegateDraftingToTheDraftServiceTest() {
		// Given: a brief and a draft service that answers
		OnePagerDraftService draftService = mock(OnePagerDraftService.class);
		OnePagerBrief brief = new OnePagerBrief("Automotive", "Acme", "Brief", List.of(OnePagerModuleKey.KPIS));
		OnePagerDraft expected = OnePagerFixtures.headerOnly();
		when(draftService.draft(brief)).thenReturn(expected);

		// When-Then: the same draft comes back
		assertThat(new OnePagerServiceImpl(draftService, mock(OnePagerDeckService.class),
				mock(GoogleDeckService.class), mock(CategoryAnalysisService.class)).draft(brief)).isSameAs(expected);
	}

	@Test
	void shouldTitleTheFileRecordItAndReturnTheSlidesUrlTest() {
		// Given: a reviewed page, a deck service that saves it, and an entity service that records it
		OnePagerDeckService deckService = mock(OnePagerDeckService.class);
		CategoryAnalysisService categoryAnalysisService = mock(CategoryAnalysisService.class);
		OnePagerDraft content = OnePagerFixtures.headerOnly();
		when(deckService.createDeck("user_1", "One Page Generator — Automotive — Acme", OnePagerShell.TOP_BAR, content))
				.thenReturn("https://docs.google.com/presentation/d/new_1/edit");
		CategoryAnalysisRecord saved = new CategoryAnalysisRecord(41L, "One Page Generator — Automotive — Acme",
				"Automotive", null, null, null, null, null, null,
				"https://docs.google.com/presentation/d/new_1/edit", "ONE_PAGER", "SUBMITTED", "user_1",
				LocalDateTime.of(2026, 10, 21, 9, 0));
		when(categoryAnalysisService.recordOnePager(new RecordOnePagerCommand(
				"One Page Generator — Automotive — Acme", "Automotive",
				"https://docs.google.com/presentation/d/new_1/edit", "user_1")))
				.thenReturn(saved);

		// When: the page is created
		OnePagerDeck deck = new OnePagerServiceImpl(mock(OnePagerDraftService.class), deckService,
				mock(GoogleDeckService.class), categoryAnalysisService)
				.createDeck(new CreateOnePagerCommand("Automotive", "Acme", OnePagerShell.TOP_BAR, content, "user_1"));

		// Then: the user gets the recorded row id, the title, and the editable link, and the row
		// was recorded only after the page existed
		assertThat(deck).isEqualTo(new OnePagerDeck(41L, "One Page Generator — Automotive — Acme",
				"https://docs.google.com/presentation/d/new_1/edit"));
		ArgumentCaptor<RecordOnePagerCommand> recorded = ArgumentCaptor.forClass(RecordOnePagerCommand.class);
		verify(categoryAnalysisService).recordOnePager(recorded.capture());
		assertThat(recorded.getValue().category()).isEqualTo("Automotive");
		assertThat(recorded.getValue().createdBy()).isEqualTo("user_1");
	}

	@Test
	void shouldNotRecordARowWhenThePageCouldNotBeCreatedTest() {
		// Given: a deck service that fails before any file exists
		OnePagerDeckService deckService = mock(OnePagerDeckService.class);
		CategoryAnalysisService categoryAnalysisService = mock(CategoryAnalysisService.class);
		OnePagerDraft content = OnePagerFixtures.headerOnly();
		when(deckService.createDeck("user_1", "One Page Generator — Automotive — Acme", OnePagerShell.TOP_BAR, content))
				.thenThrow(new IllegalStateException("Google refused"));

		// When-Then: the failure propagates and nothing is recorded
		assertThatThrownBy(() -> new OnePagerServiceImpl(mock(OnePagerDraftService.class), deckService,
				mock(GoogleDeckService.class), categoryAnalysisService)
				.createDeck(new CreateOnePagerCommand("Automotive", "Acme", OnePagerShell.TOP_BAR, content, "user_1")))
				.isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(categoryAnalysisService);
	}
}
