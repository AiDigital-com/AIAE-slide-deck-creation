package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyPromptComposerImpl;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import com.aidigital.strategyplanning.service.common.files.SourceImageType;
import com.aidigital.strategyplanning.service.common.files.impl.SourceImageServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
		return new CaseStudyPromptComposerImpl(properties, new ObjectMapper(), new SourceImageServiceImpl());
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
		String prompt = readPrompt(service.buildRequestBody(documents, List.of()));

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
				List.of(new SourceDocument("a.pdf", "body")), List.of()));

		// Then: model and JSON response format are as the contract expects, and the
		// user message is a single text part
		assertThat(payload.get("model").asText()).isEqualTo("gpt-4o-mini");
		assertThat(payload.get("response_format").get("type").asText()).isEqualTo("json_object");
		assertThat(payload.has("temperature")).isFalse();
		assertThat(payload.has("max_tokens")).isFalse();
		assertThat(payload.get("messages").get(0).get("role").asText()).isEqualTo("user");
		assertThat(payload.get("messages").get(0).get("content")).hasSize(1);
		assertThat(payload.get("messages").get(0).get("content").get(0).get("type").asText())
				.isEqualTo("text");
	}

	@Test
	void shouldAttachEveryScreenshotAsAnInlineImagePartAfterThePromptTest() {
		// Given: one document and two screenshots
		CaseStudyPromptComposerImpl service = composer("gpt-4o");
		byte[] pngBytes = "png".getBytes(StandardCharsets.UTF_8);
		byte[] webpBytes = "webp".getBytes(StandardCharsets.UTF_8);
		List<SourceImage> images = List.of(
				new SourceImage("dashboard.png", SourceImageType.PNG, pngBytes),
				new SourceImage("funnel.webp", SourceImageType.WEBP, webpBytes));

		// When: the draft request body is built
		JsonNode content = readTree(service.buildRequestBody(
				List.of(new SourceDocument("brief.pdf", "body")), images)).get("messages").get(0).get("content");

		// Then: the prompt names the screenshots and each image follows as a high-detail data URL
		assertThat(content).hasSize(3);
		assertThat(content.get(0).get("type").asText()).isEqualTo("text");
		assertThat(content.get(0).get("text").asText())
				.contains("screenshots (dashboard.png, funnel.webp); read figures from them too.")
				.contains("=== Document: brief.pdf ===");
		assertThat(content.get(1).get("type").asText()).isEqualTo("image_url");
		assertThat(content.get(1).get("image_url").get("url").asText())
				.isEqualTo("data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes));
		assertThat(content.get(1).get("image_url").get("detail").asText()).isEqualTo("high");
		assertThat(content.get(2).get("image_url").get("url").asText())
				.isEqualTo("data:image/webp;base64," + Base64.getEncoder().encodeToString(webpBytes));
	}

	@Test
	void shouldDraftFromScreenshotsAloneWithoutMentioningThemWhenAbsentTest() {
		// Given: a prompt with no documents and no images
		CaseStudyPromptComposerImpl service = composer("gpt-4o");

		// When: the draft request body is built
		String prompt = readPrompt(service.buildRequestBody(List.of(), List.of()));

		// Then: the screenshot hint is omitted and no document block is rendered
		assertThat(prompt).doesNotContain("screenshots").doesNotContain("=== Document:");
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
		JsonNode mapping = readTree(service.buildTokenMappingRequestBody(command("We cut spend.", "40% cost reduction")));
		assertThat(mapping.has("temperature")).isFalse();
		assertThat(mapping.has("max_tokens")).isFalse();
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
	 * Reads the assembled user prompt out of a serialized chat completions request body. The
	 * drafting message carries a content-parts array whose first part is the text; the token
	 * mapping message carries a plain string.
	 *
	 * @param requestBody serialized request body
	 * @return the user message text
	 */
	private String readPrompt(String requestBody) {
		JsonNode content = readTree(requestBody).get("messages").get(0).get("content");
		return content.isArray() ? content.get(0).get("text").asText() : content.asText();
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
