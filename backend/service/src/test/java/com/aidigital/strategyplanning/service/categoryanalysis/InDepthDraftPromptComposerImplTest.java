package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthDraftPromptComposerImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthTemplateTokenRegistryImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InDepthDraftPromptComposerImpl}.
 */
class InDepthDraftPromptComposerImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final CategoryAnalysisProperties properties = new CategoryAnalysisProperties();
	private final InDepthTemplateTokenRegistryImpl registry = new InDepthTemplateTokenRegistryImpl();
	private final InDepthDraftPromptComposerImpl composer =
			new InDepthDraftPromptComposerImpl(properties, objectMapper, registry);

	@Test
	void shouldDraftAllFieldsInJsonModeWithoutWebSearchTest() throws Exception {
		// Given: slide 2's tokens and a readable website
		properties.setOpenaiModel("gpt-test");
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);

		// When: the knowledge-draft body is built
		String body = composer.buildAllFieldsRequestBody("Regional Travel", "Acme Resorts", "focus on families",
				"https://acmeresorts.com", "Acme Resorts operates lakeside family holiday resorts.", tokens);
		JsonNode root = objectMapper.readTree(body);

		// Then: strict JSON mode is used, no tool is attached, and every token and brief detail is named
		assertThat(root.has("tools")).isFalse();
		assertThat(root.path("model").asText()).isEqualTo("gpt-test");
		assertThat(root.path("response_format").path("type").asText()).isEqualTo("json_object");
		assertThat(root.has("temperature")).isFalse();
		assertThat(root.has("max_tokens")).isFalse();
		assertThat(root.path("max_completion_tokens").asInt())
				.isEqualTo(InDepthDraftPromptComposerImpl.MAX_OUTPUT_TOKENS);
		String content = root.path("messages").path(0).path("content").asText();
		assertThat(content)
				.contains("Regional Travel")
				.contains("Acme Resorts")
				.contains("focus on families")
				.contains("https://acmeresorts.com")
				.contains("lakeside family holiday resorts")
				.contains("fact-checking pass will verify")
				.contains("Narrative arc");
		for (InDepthTokenSpec spec : tokens) {
			assertThat(content).contains(spec.key());
		}
	}

	@Test
	void shouldSayWhenTheWebsiteCouldNotBeReadTest() throws Exception {
		// Given: no website text
		// When: the knowledge-draft body is built
		String body = composer.buildAllFieldsRequestBody("Regional Travel", "Acme Resorts", null,
				"https://unreachable.example", null, registry.tokensForSlide(2));

		// Then: the model is told to write generically
		String content = objectMapper.readTree(body).path("messages").path(0).path("content").asText();
		assertThat(content).contains("could not be retrieved").doesNotContain("Focus notes");
	}

	@Test
	void shouldVerifyWithTheWebSearchToolAndQuoteDraftedValuesTest() throws Exception {
		// Given: one drafted stat
		InDepthTokenSpec statValue = tokenByKey("trend_stat_1_value");
		Map<String, InDepthDraftField> draftedByKey = new LinkedHashMap<>();
		draftedByKey.put(statValue.key(),
				new InDepthDraftField(statValue.key(), statValue.label(), statValue.slideNumbers(), "11.9M"));

		// When: the fact-check body is built
		JsonNode root = objectMapper.readTree(
				composer.buildVerifyRequestBody("Regional Travel", List.of(statValue, tokenByKey("trend_sources")),
						draftedByKey));

		// Then: web_search is enabled and the drafted value and its budget are quoted
		assertThat(root.path("tools").get(0).path("type").asText()).isEqualTo("web_search");
		assertThat(root.path("max_output_tokens").asInt()).isEqualTo(InDepthDraftPromptComposerImpl.MAX_OUTPUT_TOKENS);
		assertThat(root.has("temperature")).isFalse();
		assertThat(root.has("max_tokens")).isFalse();
		String input = root.path("input").asText();
		assertThat(input).contains("Regional Travel").contains(statValue.key()).contains("\"11.9M\"")
				.contains("max " + registry.maxCharsFor(statValue) + " characters")
				.contains("trend_sources (Sources): \"\"");
	}

	@Test
	void shouldCheckAlignmentInJsonModeWithoutToolsTest() throws Exception {
		// Given: a readable website
		// When: the alignment body is built
		JsonNode root = objectMapper.readTree(composer.buildAlignmentRequestBody("Boots", "Alberta Boot",
				"https://albertaboot.com", "Handmade western boots since 1978."));

		// Then: JSON output mode, no tools, website excerpt included
		assertThat(root.has("tools")).isFalse();
		assertThat(root.path("text").path("format").path("type").asText()).isEqualTo("json_object");
		assertThat(root.has("temperature")).isFalse();
		assertThat(root.has("max_tokens")).isFalse();
		assertThat(root.path("input").asText()).contains("Handmade western boots").contains("alignmentMatches");
	}

	@Test
	void shouldInferBusinessFocusWhenTheWebsiteIsUnavailableTest() throws Exception {
		// Given: no website text
		// When: the alignment body is built
		JsonNode root = objectMapper.readTree(composer.buildAlignmentRequestBody("Boots", "Alberta Boot",
				"https://albertaboot.com", null));

		// Then: the model is told to infer the focus
		assertThat(root.path("input").asText()).contains("could not be retrieved");
	}

	@Test
	void shouldRedraftOneSlideWithDeckContextAndWebSearchTest() throws Exception {
		// Given: current values for the checked deck, including a null entry and a long value
		List<InDepthFieldValue> current = Arrays.asList(
				new InDepthFieldValue("trend_headline", "GROWING FAST"),
				new InDepthFieldValue("trend_intro", "x".repeat(80)),
				null,
				new InDepthFieldValue(null, "ignored"),
				new InDepthFieldValue("hi_headline", null));
		List<InDepthTokenSpec> slideTokens = registry.tokensForSlide(16);

		// When: the redraft body is built
		JsonNode root = objectMapper.readTree(composer.buildSlideRequestBody("Regional Travel", "Acme",
				"families", 16, "make it Gen Z", slideTokens, current));

		// Then: web_search is on, the brief, note, and abbreviated context are present, and the slide's keys are listed
		assertThat(root.path("tools").get(0).path("type").asText()).isEqualTo("web_search");
		String input = root.path("input").asText();
		assertThat(input).contains("Regional Travel").contains("Acme").contains("families")
				.contains("make it Gen Z").contains("trend_headline: GROWING FAST")
				.contains("x".repeat(57) + "...").contains("hi_headline: ")
				.doesNotContain("ignored");
		for (InDepthTokenSpec spec : slideTokens) {
			assertThat(input).contains(spec.key());
		}
	}

	@Test
	void shouldOmitContextWhenNoCurrentFieldsAreSuppliedTest() throws Exception {
		// Given: a redraft with no deck context and no notes
		// When: the redraft body is built
		JsonNode root = objectMapper.readTree(composer.buildSlideRequestBody("Regional Travel", "Acme",
				null, 2, null, registry.tokensForSlide(2), null));

		// Then: the context section is left out
		assertThat(root.path("input").asText()).doesNotContain("Current content of the checked deck")
				.doesNotContain("Original brief").doesNotContain("Guidance for THIS redraft");
	}

	private InDepthTokenSpec tokenByKey(String key) {
		return InDepthTemplateTokens.TOKENS.stream().filter(t -> t.key().equals(key)).findFirst().orElseThrow();
	}
}
