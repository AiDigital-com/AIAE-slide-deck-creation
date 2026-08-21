package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyDraftParserImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyFallbackTokenResolverImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyPromptComposerImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OpenAiCaseStudyDraftServiceImpl;
import com.aidigital.strategyplanning.service.casestudy.templates.CaseStudyTemplateTokens;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setOpenaiApiKey(apiKey);
		properties.setOpenaiModel(model);
		ObjectMapper objectMapper = new ObjectMapper();
		return new OpenAiCaseStudyDraftServiceImpl(properties, objectMapper,
				mock(OpenAiChatClient.class),
				new CaseStudyPromptComposerImpl(properties, objectMapper),
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
		assertThatThrownBy(() -> service.draft(List.of(new SourceDocument("a.pdf", "body"))))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C003.getCode())
				.hasMessageContaining("OPENAI_API_KEY");
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
