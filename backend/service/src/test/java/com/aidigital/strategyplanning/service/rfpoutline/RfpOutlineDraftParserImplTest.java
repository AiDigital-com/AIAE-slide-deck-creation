package com.aidigital.strategyplanning.service.rfpoutline;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineDraft;
import com.aidigital.strategyplanning.service.rfpoutline.services.impl.RfpOutlineDraftParserImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for parsing the AI's RFP outline draft.
 *
 * <p>The model's answer is untrusted input: a missing, null, or whitespace-only section must come
 * back as null so the review form shows an empty field the user can fill, rather than a blank
 * string that looks answered.
 */
class RfpOutlineDraftParserImplTest {

	@Test
	void shouldReadEverySectionFromTheDraftTest() {
		// Given: a complete draft from the model
		RfpOutlineDraftParserImpl parser = new RfpOutlineDraftParserImpl(new ObjectMapper());
		String content = "{\"title\":\"Globex RFP\",\"clientName\":\"Globex\","
				+ "\"industry\":\"Logistics\",\"challenge\":\"Fragmented tracking.\","
				+ "\"opportunity\":\"Unified visibility.\",\"solution\":\"One control tower.\","
				+ "\"outcome\":\"On-time delivery up.\",\"deckOutline\":\"1. Context\"}";

		// When: it is parsed
		RfpOutlineDraft draft = parser.parseDraft(content, "globex-rfp.pdf");

		// Then: every section reaches the review form, and the source file names are carried
		// through unchanged rather than being taken from the model's answer
		assertThat(draft.title()).isEqualTo("Globex RFP");
		assertThat(draft.clientName()).isEqualTo("Globex");
		assertThat(draft.industry()).isEqualTo("Logistics");
		assertThat(draft.challenge()).isEqualTo("Fragmented tracking.");
		assertThat(draft.opportunity()).isEqualTo("Unified visibility.");
		assertThat(draft.solution()).isEqualTo("One control tower.");
		assertThat(draft.outcome()).isEqualTo("On-time delivery up.");
		assertThat(draft.deckOutline()).isEqualTo("1. Context");
		assertThat(draft.sourceFileNames()).isEqualTo("globex-rfp.pdf");
	}

	@Test
	void shouldTrimSurroundingWhitespaceFromEachSectionTest() {
		// Given: a draft whose values carry the model's own padding
		RfpOutlineDraftParserImpl parser = new RfpOutlineDraftParserImpl(new ObjectMapper());

		// When: it is parsed
		RfpOutlineDraft draft =
				parser.parseDraft("{\"title\":\"  Globex RFP  \"}", "globex-rfp.pdf");

		// Then: the stored value has no stray padding, which would show up in the Google Doc
		assertThat(draft.title()).isEqualTo("Globex RFP");
	}

	@Test
	void shouldTreatMissingNullAndBlankSectionsAsAbsentTest() {
		// Given: a draft where the model skipped, nulled, and blanked different sections
		RfpOutlineDraftParserImpl parser = new RfpOutlineDraftParserImpl(new ObjectMapper());
		String content = "{\"title\":\"Globex RFP\",\"clientName\":null,\"industry\":\"   \"}";

		// When: it is parsed
		RfpOutlineDraft draft = parser.parseDraft(content, null);

		// Then: all three arrive as null, so the review form shows them as unanswered instead
		// of as an empty answer the user might not notice
		assertThat(draft.title()).isEqualTo("Globex RFP");
		assertThat(draft.clientName()).isNull();
		assertThat(draft.industry()).isNull();
		assertThat(draft.challenge()).isNull();
		assertThat(draft.sourceFileNames()).isNull();
	}

	@Test
	void shouldRejectAnAnswerThatIsNotJsonTest() {
		// Given: the model answered in prose instead of JSON
		RfpOutlineDraftParserImpl parser = new RfpOutlineDraftParserImpl(new ObjectMapper());

		// When-Then: the caller gets the external-call error rather than a half-filled draft
		assertThatThrownBy(() -> parser.parseDraft("Sure! Here is your outline:", "rfp.pdf"))
				.isInstanceOf(AppException.class)
				.satisfies(e -> {
					AppException failure = (AppException) e;
					assertThat(failure.getValidationMessage().getCode()).isEqualTo("C003");
					assertThat(failure.getMessage()).contains("AI draft was not valid JSON");
				});
	}
}
