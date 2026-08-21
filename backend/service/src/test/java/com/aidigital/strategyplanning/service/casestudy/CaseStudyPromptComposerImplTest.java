package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyPromptComposerImpl;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization tests for {@link CaseStudyPromptComposerImpl}.
 *
 * <p>The two prompts decide what the model drafts and in what shape it replies, so their
 * composition and the payload envelope are asserted explicitly.
 */
class CaseStudyPromptComposerImplTest {

	private CaseStudyPromptComposerImpl composer(String model) {
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setOpenaiModel(model);
		return new CaseStudyPromptComposerImpl(properties, new ObjectMapper());
	}

	private CreateCaseStudyCommand command(String results, String keyMetrics) {
		return new CreateCaseStudyCommand("Acme rollout", "Acme", "Retail", "Costs were rising",
				"We rebuilt the funnel", results, keyMetrics, "Q1 2026", "Great partner",
				"brief.pdf", "user_123");
	}

	@Test
	void shouldIncludeEverySourceDocumentInTheDraftPromptTest() {
		// Given: two source documents
		CaseStudyPromptComposerImpl service = composer("gpt-4o");
		List<SourceDocument> documents = List.of(
				new SourceDocument("report.xlsx", "Q1 | 40% | 2x"),
				new SourceDocument("notes.docx", "Client praised the team."));

		// When: the draft request body is built
		String prompt = readPrompt(service.buildRequestBody(documents));

		// Then: both documents appear with their filenames, plus the drafting rules
		assertThat(prompt).contains("=== Document: report.xlsx ===", "Q1 | 40% | 2x");
		assertThat(prompt).contains("=== Document: notes.docx ===", "Client praised the team.");
		assertThat(prompt).contains("You are a senior strategist at a digital marketing agency.");
		assertThat(prompt).contains("Use ONLY facts present in the documents");
		assertThat(prompt).contains("title, clientName, industry, challenge, solution, results, "
				+ "keyMetrics, timeline, testimonial. Return only the JSON object.");
	}

	@Test
	void shouldPinTheDraftPayloadShapeTest() {
		// Given: the configured model
		CaseStudyPromptComposerImpl service = composer("gpt-4o-mini");

		// When: the draft request body is built
		JsonNode payload = readTree(service.buildRequestBody(
				List.of(new SourceDocument("a.pdf", "body"))));

		// Then: model, JSON response format, and temperature are as the contract expects
		assertThat(payload.get("model").asText()).isEqualTo("gpt-4o-mini");
		assertThat(payload.get("response_format").get("type").asText()).isEqualTo("json_object");
		assertThat(payload.get("temperature").asDouble()).isEqualTo(0.3);
		assertThat(payload.get("messages").get(0).get("role").asText()).isEqualTo("user");
	}

	@Test
	void shouldCarryEveryReviewedFieldIntoTheTokenMappingPromptTest() {
		// Given: a fully populated command
		CaseStudyPromptComposerImpl service = composer("gpt-4o");

		// When: the token mapping request body is built
		String prompt = readPrompt(service.buildTokenMappingRequestBody(
				command("We cut spend.", "40% cost reduction")));

		// Then: every labelled field and the placeholder contract are present
		assertThat(prompt).contains("Title: Acme rollout", "Client: Acme", "Industry: Retail",
				"Challenge: Costs were rising", "Solution: We rebuilt the funnel",
				"Results: We cut spend.", "Key metrics: 40% cost reduction",
				"Timeline: Q1 2026", "Testimonial: Great partner");
		assertThat(prompt).contains("Use ONLY facts from the fields; never invent numbers or claims.");
		assertThat(prompt).contains("- priority_1, priority_2, priority_3");
		assertThat(prompt).endsWith("Return only the JSON object.");
	}

	@Test
	void shouldRenderNullReviewedFieldsAsEmptyInThePromptTest() {
		// Given: a command whose optional fields are null
		CaseStudyPromptComposerImpl service = composer("gpt-4o");
		CreateCaseStudyCommand sparse = new CreateCaseStudyCommand("Only a title", null, null,
				null, null, null, null, null, null, null, "user_123");

		// When: the token mapping request body is built
		String prompt = readPrompt(service.buildTokenMappingRequestBody(sparse));

		// Then: the labels remain with empty values rather than the literal "null"
		assertThat(prompt).contains("Title: Only a title", "Client: \n", "Industry: \n");
		assertThat(prompt).doesNotContain("null");
	}

	/**
	 * Reads the assembled user prompt out of a serialized chat completions request body.
	 *
	 * @param requestBody serialized request body
	 * @return the user message content
	 */
	private String readPrompt(String requestBody) {
		return readTree(requestBody).get("messages").get(0).get("content").asText();
	}

	/**
	 * Parses a serialized request body, failing the test on malformed JSON.
	 *
	 * @param requestBody serialized request body
	 * @return parsed payload
	 */
	private JsonNode readTree(String requestBody) {
		try {
			return new ObjectMapper().readTree(requestBody);
		} catch (Exception e) {
			throw new AssertionError("request body was not valid JSON", e);
		}
	}
}
