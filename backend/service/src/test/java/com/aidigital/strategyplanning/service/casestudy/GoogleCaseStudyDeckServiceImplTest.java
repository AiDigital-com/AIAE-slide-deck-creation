package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.external.clerk.ClerkOAuthClient;
import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.external.google.GoogleWorkspaceClient;
import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyDraftParserImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyFallbackTokenResolverImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyPromptComposerImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.GoogleCaseStudyDeckServiceImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OpenAiCaseStudyDraftServiceImpl;
import com.aidigital.strategyplanning.service.casestudy.templates.CaseStudyTemplateTokens;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.google.impl.GoogleGrantServiceImpl;
import com.aidigital.strategyplanning.service.common.google.impl.GoogleTemplateFileGatewayImpl;
import com.aidigital.strategyplanning.service.common.google.impl.TokenReplacementRequestFactoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the case study Google deck filler and the deterministic token mapping fallback.
 */
class GoogleCaseStudyDeckServiceImplTest {

	private static final String ONE_TOKEN_BODY =
			"{\"requests\":[{\"replaceAllText\":{\"containsText\":{\"text\":"
					+ "\"{{client_vertical}}\",\"matchCase\":true},\"replaceText\":\"Retail\"}}]}";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void shouldFailWithC003WhenNoClerkSecretIsConfiguredTest() {
		// Given: the feature has no Clerk secret, so no grant can be looked up
		GoogleCaseStudyDeckServiceImpl service = new GoogleCaseStudyDeckServiceImpl(
				new CaseStudyProperties(),
				new GoogleGrantServiceImpl(mock(ClerkOAuthClient.class)),
				new GoogleTemplateFileGatewayImpl(objectMapper, mock(GoogleWorkspaceClient.class)),
				new TokenReplacementRequestFactoryImpl(objectMapper));

		// When-Then: the caller sees the external-call error code, not a 500
		assertThatThrownBy(() -> service.createDeck("user_1", "Deck", Map.of()))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getCode())
						.isEqualTo("C003"));
	}

	@Test
	void shouldTellTheUserToSignInWithGoogleWhenNoGrantExistsTest() {
		// Given: the user has never connected Google
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setClerkSecretKey("sk_test");
		ClerkOAuthClient clerkClient = mock(ClerkOAuthClient.class);
		when(clerkClient.fetchGrant("sk_test", "user_1", "oauth_google")).thenReturn(null);
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		GoogleCaseStudyDeckServiceImpl service = new GoogleCaseStudyDeckServiceImpl(properties,
				new GoogleGrantServiceImpl(clerkClient),
				new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient),
				new TokenReplacementRequestFactoryImpl(objectMapper));

		// When-Then: the message tells them what to do, and Google is never called
		assertThatThrownBy(() -> service.createDeck("user_1", "Deck", Map.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Not signed in with Google — sign in with your Google account "
						+ "to create the deck");
		verifyNoInteractions(workspaceClient);
	}

	@Test
	void shouldAskForAReconnectWhenTheSlidesScopeIsMissingTest() {
		// Given: a grant that covers Drive and Docs but not Slides
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setClerkSecretKey("sk_test");
		ClerkOAuthClient clerkClient = mock(ClerkOAuthClient.class);
		when(clerkClient.fetchGrant("sk_test", "user_1", "oauth_google"))
				.thenReturn(new ClerkOAuthGrant("ya29.token",
						List.of("https://www.googleapis.com/auth/drive",
								"https://www.googleapis.com/auth/documents")));
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		GoogleCaseStudyDeckServiceImpl service = new GoogleCaseStudyDeckServiceImpl(properties,
				new GoogleGrantServiceImpl(clerkClient),
				new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient),
				new TokenReplacementRequestFactoryImpl(objectMapper));

		// When-Then: a doc-shaped grant does not silently produce a broken deck
		assertThatThrownBy(() -> service.createDeck("user_1", "Deck", Map.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Google Drive & Slides access not granted — reconnect Google "
						+ "and allow Drive & Slides access");
		verifyNoInteractions(workspaceClient);
	}

	@Test
	void shouldCopyTheTemplateAndReturnThePresentationUrlTest() {
		// Given: a fully granted user and a Drive that copies the template
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setClerkSecretKey("sk_test");
		properties.setTemplatePresentationId("template_deck");
		ClerkOAuthClient clerkClient = mock(ClerkOAuthClient.class);
		when(clerkClient.fetchGrant("sk_test", "user_1", "oauth_google"))
				.thenReturn(new ClerkOAuthGrant("ya29.token",
						List.of("https://www.googleapis.com/auth/drive",
								"https://www.googleapis.com/auth/presentations")));
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		when(workspaceClient.copyDriveFile("ya29.token", "template_deck", "Deck"))
				.thenReturn(objectMapper.createObjectNode().put("id", "deck_1"));
		when(workspaceClient.batchUpdatePresentation("ya29.token", "deck_1", ONE_TOKEN_BODY))
				.thenReturn(objectMapper.createObjectNode());
		GoogleCaseStudyDeckServiceImpl service = new GoogleCaseStudyDeckServiceImpl(properties,
				new GoogleGrantServiceImpl(clerkClient),
				new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient),
				new TokenReplacementRequestFactoryImpl(objectMapper));

		// When: the deck is created
		String url = service.createDeck("user_1", "Deck", Map.of("client_vertical", "Retail"));

		// Then: the user gets an editable Slides link, and the copy used their own token
		assertThat(url).isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		verify(workspaceClient).copyDriveFile("ya29.token", "template_deck", "Deck");
		verify(workspaceClient).batchUpdatePresentation("ya29.token", "deck_1", ONE_TOKEN_BODY);
	}

	@Test
	void fallbackTokenValuesMapsFieldsAndMetrics() {
		OpenAiCaseStudyDraftServiceImpl draftService =
				new OpenAiCaseStudyDraftServiceImpl(new CaseStudyProperties(), objectMapper,
						mock(OpenAiChatClient.class),
						new CaseStudyPromptComposerImpl(new CaseStudyProperties(), objectMapper),
						new CaseStudyFallbackTokenResolverImpl(),
						new CaseStudyDraftParserImpl(objectMapper));
		CreateCaseStudyCommand command = new CreateCaseStudyCommand(
				"Acme Turnaround", "Acme", "Retail",
				"Sales were declining.", "We rebuilt the funnel.",
				"Revenue grew fast. Conversion doubled. Churn dropped.",
				"40% cost reduction, 2x pipeline growth", "6 months", null, null, "user_1");
		Map<String, String> values = draftService.buildTemplateTokenValues(command);
		assertThat(values.keySet()).containsExactlyElementsOf(CaseStudyTemplateTokens.TOKEN_KEYS);
		assertThat(values.get("client_vertical")).isEqualTo("Retail");
		assertThat(values.get("client_challenge")).isEqualTo("Sales were declining.");
		assertThat(values.get("solution_body")).isEqualTo("We rebuilt the funnel.");
		assertThat(values.get("results_intro")).isEqualTo("Revenue grew fast.");
		assertThat(values.get("result_detail_1")).isEqualTo("Conversion doubled.");
		assertThat(values.get("metric_1_value")).isEqualTo("40%");
		assertThat(values.get("metric_1_label")).isEqualTo("cost reduction");
	}
}
