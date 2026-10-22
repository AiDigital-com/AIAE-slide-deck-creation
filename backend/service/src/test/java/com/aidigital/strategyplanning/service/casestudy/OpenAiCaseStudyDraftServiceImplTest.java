package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyDraftParserImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyFallbackTokenResolverImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyPromptComposerImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OpenAiCaseStudyDraftServiceImpl;
import com.aidigital.strategyplanning.service.casestudy.templates.CaseStudyTemplateTokens;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import com.aidigital.strategyplanning.service.common.files.SourceImageType;
import com.aidigital.strategyplanning.service.common.files.impl.SourceImageServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Characterization tests for {@link OpenAiCaseStudyDraftServiceImpl}.
 *
 * <p>Covers what the service itself decides: whether the drafting engine is available, refusing
 * to draft without it, and falling back to the deterministic token mapping instead of failing a
 * deck. The real collaborators are wired so the fallback path is exercised end to end.
 */
class OpenAiCaseStudyDraftServiceImplTest {

	private static final String CONNECTED_KEY = "sk-test-key";

	private OpenAiCaseStudyDraftServiceImpl service(String apiKey, String model) {
		return service(apiKey, model, mock(OpenAiChatClient.class));
	}

	private OpenAiCaseStudyDraftServiceImpl service(String apiKey, String model, OpenAiChatClient client) {
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setOpenaiApiKey(apiKey);
		properties.setOpenaiBaseUrl("https://api.test/v1");
		properties.setOpenaiModel(model);
		ObjectMapper objectMapper = new ObjectMapper();
		return new OpenAiCaseStudyDraftServiceImpl(properties, objectMapper, client,
				new CaseStudyPromptComposerImpl(properties, objectMapper, new SourceImageServiceImpl()),
				new CaseStudyFallbackTokenResolverImpl(),
				new CaseStudyDraftParserImpl(objectMapper));
	}

	private CreateCaseStudyCommand command(String results, String keyMetrics) {
		return new CreateCaseStudyCommand("Acme rollout", "Acme", "Retail", "Costs were rising",
				"We rebuilt the funnel", results, keyMetrics, "Q1 2026", "Great partner",
				"brief.pdf", "user_123");
	}

	@Test
	void shouldReportDisconnectedWhenNoApiKeyIsConfiguredTest() {
		// Given: no OpenAI key
		// When-Then: the engine reports itself unavailable
		assertThat(service("", "gpt-4o").isConnected()).isFalse();
		assertThat(service(null, "gpt-4o").isConnected()).isFalse();
		assertThat(service(CONNECTED_KEY, "gpt-4o").isConnected()).isTrue();
	}

	@Test
	void shouldRefuseToDraftWhenTheEngineIsNotConnectedTest() {
		// Given: an unconfigured drafting engine
		OpenAiCaseStudyDraftServiceImpl service = service("", "gpt-4o");

		// When-Then: drafting fails with the external-call reason and names the missing key
		assertThatThrownBy(() -> service.draft(List.of(new SourceDocument("a.pdf", "body")), List.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C003.getCode())
				.hasMessageContaining("OPENAI_API_KEY");
	}

	@Test
	void shouldSendDocumentsAndScreenshotsInOneCallAndNameEverySourceTest() throws Exception {
		// Given: a connected engine whose reply drafts a title, and one document plus one screenshot
		OpenAiChatClient client = mock(OpenAiChatClient.class);
		when(client.complete(argThat(call -> call != null && call.apiKey().equals(CONNECTED_KEY))))
				.thenReturn("{\"title\":\"Acme rollout\",\"keyMetrics\":\"40% cost reduction\"}");
		OpenAiCaseStudyDraftServiceImpl service = service(CONNECTED_KEY, "gpt-4o", client);
		SourceImage screenshot = new SourceImage("dashboard.png", SourceImageType.PNG, new byte[]{1, 2});

		// When: a draft is requested
		CaseStudyDraft draft = service.draft(List.of(new SourceDocument("brief.pdf", "Body")),
				List.of(screenshot));

		// Then: one call carried the document text and the image part, and both sources are named
		ArgumentCaptor<OpenAiChatCall> call = ArgumentCaptor.forClass(OpenAiChatCall.class);
		verify(client).complete(call.capture());
		assertThat(call.getValue().baseUrl()).isEqualTo("https://api.test/v1");
		JsonNode content = new ObjectMapper().readTree(call.getValue().requestBody())
				.get("messages").get(0).get("content");
		assertThat(content).hasSize(2);
		assertThat(content.get(0).get("text").asText()).contains("=== Document: brief.pdf ===", "Body");
		assertThat(content.get(1).get("image_url").get("url").asText()).startsWith("data:image/png;base64,");
		assertThat(draft.title()).isEqualTo("Acme rollout");
		assertThat(draft.keyMetrics()).isEqualTo("40% cost reduction");
		assertThat(draft.sourceFileNames()).isEqualTo("brief.pdf, dashboard.png");
	}

	@Test
	void shouldSeedEveryTemplateTokenInTheFallbackTest() {
		// Given: the AI engine is unavailable
		OpenAiCaseStudyDraftServiceImpl service = service("", "gpt-4o");

		// When: token values are built for a reviewed case study
		Map<String, String> values = service.buildTemplateTokenValues(
				command("We cut spend.", "40% cost reduction"));

		// Then: every template token is present, so no {{token}} survives on the slide
		assertThat(values.keySet()).containsExactlyElementsOf(CaseStudyTemplateTokens.TOKEN_KEYS);
		assertThat(values.values()).doesNotContainNull();
	}
}
