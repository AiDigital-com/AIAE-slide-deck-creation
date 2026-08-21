package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyDraftParserImpl;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Characterization tests for {@link CaseStudyDraftParserImpl}.
 *
 * <p>Parsing is deliberately tolerant: a missing field, an explicit {@code null}, and a
 * whitespace-only value all mean "no value" rather than an empty string on the slide.
 */
class CaseStudyDraftParserImplTest {

	@Test
	void shouldParseTheDraftAndKeepTheSourceFileNamesTest() {
		// Given: a complete JSON reply
		CaseStudyDraftParserImpl service = new CaseStudyDraftParserImpl(new ObjectMapper());
		String content = "{\"title\":\"Acme rollout\",\"clientName\":\"Acme\","
				+ "\"industry\":\"Retail\",\"challenge\":\"Costs\",\"solution\":\"Funnel\","
				+ "\"results\":\"Better\",\"keyMetrics\":\"40%\",\"timeline\":\"Q1\","
				+ "\"testimonial\":\"Great\"}";

		// When: the reply is parsed
		CaseStudyDraft draft = service.parseDraft(content, "a.pdf, b.pdf");

		// Then: every field maps across and the filenames are carried through untouched
		assertThat(draft).isEqualTo(new CaseStudyDraft("Acme rollout", "Acme", "Retail", "Costs",
				"Funnel", "Better", "40%", "Q1", "Great", "a.pdf, b.pdf"));
	}

	@Test
	void shouldTreatMissingNullAndBlankRepliesAsAbsentFieldsTest() {
		// Given: a reply with a null, a blank, a whitespace-only, and an absent field
		CaseStudyDraftParserImpl service = new CaseStudyDraftParserImpl(new ObjectMapper());
		String content = "{\"title\":\"Kept\",\"clientName\":null,\"industry\":\"\","
				+ "\"challenge\":\"   \"}";

		// When: the reply is parsed
		CaseStudyDraft draft = service.parseDraft(content, "a.pdf");

		// Then: only the real value survives; the rest become null rather than empty strings
		assertThat(draft.title()).isEqualTo("Kept");
		assertThat(draft.clientName()).isNull();
		assertThat(draft.industry()).isNull();
		assertThat(draft.challenge()).isNull();
		assertThat(draft.solution()).isNull();
	}

	@Test
	void shouldTrimSurroundingWhitespaceFromReplyValuesTest() {
		// Given: a reply whose values carry padding
		CaseStudyDraftParserImpl service = new CaseStudyDraftParserImpl(new ObjectMapper());

		// When-Then: the padding is stripped
		assertThat(service.parseDraft("{\"title\":\"  Padded  \"}", "a.pdf").title())
				.isEqualTo("Padded");
	}

	@Test
	void shouldRejectAReplyThatIsNotValidJsonTest() {
		// Given: a non-JSON reply
		CaseStudyDraftParserImpl service = new CaseStudyDraftParserImpl(new ObjectMapper());

		// When-Then: it surfaces as an external-call failure, not a parse crash
		assertThatThrownBy(() -> service.parseDraft("Sorry, I cannot help.", "a.pdf"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("AI draft was not valid JSON");
	}
}
