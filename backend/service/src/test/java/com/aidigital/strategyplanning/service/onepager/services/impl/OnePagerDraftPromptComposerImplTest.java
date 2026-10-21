package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.config.OnePagerProperties;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerFieldBudget;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the drafting prompt: the brief is the only source, and only the picked modules'
 * fields are requested, each with its budget.
 */
class OnePagerDraftPromptComposerImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private OnePagerDraftPromptComposerImpl composer() {
		OnePagerProperties properties = new OnePagerProperties();
		properties.setOpenaiModel("gpt-4o");
		return new OnePagerDraftPromptComposerImpl(properties, objectMapper);
	}

	@Test
	void shouldAskForStrictJsonWithTheConfiguredModelTest() throws Exception {
		// Given: a brief with two modules
		OnePagerBrief brief = new OnePagerBrief("Automotive", "Acme", "Sell more cars.",
				List.of(OnePagerModuleKey.SOLUTIONS, OnePagerModuleKey.KPIS));

		// When: the request body is built
		JsonNode body = objectMapper.readTree(composer().buildDraftRequestBody(brief));

		// Then: JSON mode, the model, the token ceiling, and one user message carrying the prompt
		assertThat(body.path("model").asText()).isEqualTo("gpt-4o");
		assertThat(body.path("response_format").path("type").asText()).isEqualTo("json_object");
		assertThat(body.has("temperature")).isFalse();
		assertThat(body.has("max_tokens")).isFalse();
		assertThat(body.path("max_completion_tokens").asInt())
				.isEqualTo(OnePagerDraftPromptComposerImpl.MAX_COMPLETION_TOKENS);
		assertThat(OnePagerDraftPromptComposerImpl.MAX_COMPLETION_TOKENS).isEqualTo(8000);
		assertThat(body.path("messages")).hasSize(1);
		assertThat(body.path("messages").path(0).path("role").asText()).isEqualTo("user");
		assertThat(body.path("messages").path(0).path("content").asText())
				.contains("\"Automotive\" category, prepared for Acme")
				.contains("Sell more cars.");
	}

	@Test
	void shouldListOnlyThePickedModulesFieldsWithTheirBudgetsTest() {
		// Given: a brief with solutions (no visual) and the milestone banner
		OnePagerBrief brief = new OnePagerBrief("Retail", "Acme", "Brief",
				List.of(OnePagerModuleKey.SOLUTIONS, OnePagerModuleKey.MILESTONE_BANNER));

		// When: the prompt is built
		String prompt = composer().buildPrompt(brief);

		// Then: header, solutions, and milestones are requested with budgets; the rest are absent
		assertThat(prompt)
				.contains("- topicPill: header pill label (max " + OnePagerFieldBudget.TOPIC_PILL.getMaxChars())
				.contains("- mainTitle: an array of 1-3 short strings")
				.contains("- solutions: an array of exactly " + OnePagerDraftPromptComposerImpl.SOLUTION_CARD_COUNT)
				.contains("    - body: explanatory copy (max " + OnePagerFieldBudget.SOLUTION_BODY.getMaxChars())
				.contains("- milestoneHeadline:")
				.contains("    - subtext: a short supporting subtext (max "
						+ OnePagerFieldBudget.MILESTONE_SUBTEXT.getMaxChars())
				.contains("NEVER answer any field with a placeholder");
		assertThat(prompt).doesNotContain("solutionsVisualLabel", "processSteps", "kpis:", "imageTextBody",
				"keyTakeaway", "impactStats", "capabilityCards", "spotlightCards", "insightCards");
	}

	@Test
	void shouldRequestEveryOptionalFieldWhenEveryModuleIsPickedTest() {
		// Given: a brief with every module
		OnePagerBrief brief = new OnePagerBrief("Retail", "Acme", "Brief", List.of(OnePagerModuleKey.values()));

		// When: the prompt is built
		String prompt = composer().buildPrompt(brief);

		// Then: every module's keys are requested, including the chip and stage-name rules
		assertThat(prompt).contains("processSteps", "solutionsVisualLabel", "kpiDisclaimer", "imageTextBody",
				"keyTakeaway", "impactStats", "capabilityCards", "spotlightCards", "kpiChips: ONLY for index 1",
				"stageNames: ONLY for index 2", "insightCards", "milestones");
		assertThat(prompt).contains("max " + OnePagerFieldBudget.KPI_VALUE.getMaxChars()
				+ " characters, NEVER a phrase or sentence");
	}
}
