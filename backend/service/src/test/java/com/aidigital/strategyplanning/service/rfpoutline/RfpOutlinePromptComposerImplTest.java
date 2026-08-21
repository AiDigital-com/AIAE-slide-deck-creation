package com.aidigital.strategyplanning.service.rfpoutline;

import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.rfpoutline.config.RfpOutlineProperties;
import com.aidigital.strategyplanning.service.rfpoutline.services.impl.RfpOutlinePromptComposerImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization tests for the RFP outline prompt assembly.
 *
 * <p>These pin the prompt that {@code buildRequestBody} produces after it was split into
 * {@code appendStrategistFraming}, {@code appendSourceMaterial}, and
 * {@code appendDraftingInstructions}. The prompt is product behaviour — it determines what the
 * model drafts — so the composition order and the presence of each section are asserted
 * explicitly rather than left to a green compile.
 */
class RfpOutlinePromptComposerImplTest {

	private static final String FRAMING_MARKER = "You are a senior strategist at AI Digital";
	private static final String INSTRUCTION_MARKER = "Draft two things:";
	private static final String SCHEMA_MARKER = "Return only the JSON object.";

	@Test
	void shouldComposeFramingThenSourceMaterialThenInstructionsTest() {
		// Given: a service with two source documents and supplementary notes
		ObjectMapper objectMapper = new ObjectMapper();
		RfpOutlineProperties properties = new RfpOutlineProperties();
		properties.setOpenaiModel("gpt-4o");
		RfpOutlinePromptComposerImpl service =
				new RfpOutlinePromptComposerImpl(properties, objectMapper);
		List<SourceDocument> documents = List.of(
				new SourceDocument("brief.pdf", "Client wants Spanish-first AEP outreach."),
				new SourceDocument("terms.docx", "Indemnification boilerplate."));

		// When: the request body is built
		String body = service.buildRequestBody(documents, "Budget is fixed at 1.2M.");

		// Then: the three sections appear exactly once, in composition order
		String prompt = readPrompt(objectMapper, body);
		assertThat(prompt).containsOnlyOnce(FRAMING_MARKER);
		assertThat(prompt).containsOnlyOnce(INSTRUCTION_MARKER);
		assertThat(prompt).containsOnlyOnce(SCHEMA_MARKER);
		assertThat(prompt.indexOf(FRAMING_MARKER))
				.isLessThan(prompt.indexOf("=== Document: brief.pdf ==="));
		assertThat(prompt.indexOf("=== Document: brief.pdf ==="))
				.isLessThan(prompt.indexOf(INSTRUCTION_MARKER));
	}

	@Test
	void shouldIncludeEverySourceDocumentAndTheNotesTest() {
		// Given: three documents and notes the strategist supplied
		ObjectMapper objectMapper = new ObjectMapper();
		RfpOutlineProperties properties = new RfpOutlineProperties();
		RfpOutlinePromptComposerImpl service =
				new RfpOutlinePromptComposerImpl(properties, objectMapper);
		List<SourceDocument> documents = List.of(
				new SourceDocument("one.pdf", "First body."),
				new SourceDocument("two.pdf", "Second body."),
				new SourceDocument("three.pdf", "Third body."));

		// When: the request body is built
		String body = service.buildRequestBody(documents, "Extra context from the call.");

		// Then: every filename, every extracted body, and the notes are present
		String prompt = readPrompt(objectMapper, body);
		assertThat(prompt).contains("=== Document: one.pdf ===", "First body.");
		assertThat(prompt).contains("=== Document: two.pdf ===", "Second body.");
		assertThat(prompt).contains("=== Document: three.pdf ===", "Third body.");
		assertThat(prompt).contains("=== Supplementary notes from the strategist ===",
				"Extra context from the call.");
	}

	@Test
	void shouldOmitTheNotesSectionWhenNotesAreBlankTest() {
		// Given: a blank notes value
		ObjectMapper objectMapper = new ObjectMapper();
		RfpOutlineProperties properties = new RfpOutlineProperties();
		RfpOutlinePromptComposerImpl service =
				new RfpOutlinePromptComposerImpl(properties, objectMapper);
		List<SourceDocument> documents = List.of(new SourceDocument("brief.pdf", "Body."));

		// When: the request body is built
		String body = service.buildRequestBody(documents, "   ");

		// Then: the notes heading is absent while the document survives
		String prompt = readPrompt(objectMapper, body);
		assertThat(prompt).doesNotContain("=== Supplementary notes from the strategist ===");
		assertThat(prompt).contains("=== Document: brief.pdf ===");
	}

	@Test
	void shouldRequestJsonObjectResponseForTheConfiguredModelTest() {
		// Given: an explicitly configured model
		ObjectMapper objectMapper = new ObjectMapper();
		RfpOutlineProperties properties = new RfpOutlineProperties();
		properties.setOpenaiModel("gpt-4o-mini");
		RfpOutlinePromptComposerImpl service =
				new RfpOutlinePromptComposerImpl(properties, objectMapper);

		// When: the request body is built
		String body = service.buildRequestBody(List.of(new SourceDocument("a.pdf", "b")), null);

		// Then: the payload pins model, response format, temperature, and token ceiling
		JsonNode payload = readTree(objectMapper, body);
		assertThat(payload.get("model").asText()).isEqualTo("gpt-4o-mini");
		assertThat(payload.get("response_format").get("type").asText()).isEqualTo("json_object");
		assertThat(payload.get("temperature").asDouble()).isEqualTo(0.3);
		assertThat(payload.get("max_tokens").asInt()).isEqualTo(8000);
		assertThat(payload.get("messages").get(0).get("role").asText()).isEqualTo("user");
	}

	/**
	 * Reads the assembled user prompt out of a serialized chat completions request body.
	 *
	 * @param objectMapper mapper used to parse the body
	 * @param requestBody  serialized request body
	 * @return the user message content
	 */
	private String readPrompt(ObjectMapper objectMapper, String requestBody) {
		return readTree(objectMapper, requestBody).get("messages").get(0).get("content").asText();
	}

	/**
	 * Parses a serialized request body, failing the test on malformed JSON.
	 *
	 * @param objectMapper mapper used to parse the body
	 * @param requestBody  serialized request body
	 * @return parsed payload
	 */
	private JsonNode readTree(ObjectMapper objectMapper, String requestBody) {
		try {
			return objectMapper.readTree(requestBody);
		} catch (Exception e) {
			throw new AssertionError("request body was not valid JSON", e);
		}
	}
}
