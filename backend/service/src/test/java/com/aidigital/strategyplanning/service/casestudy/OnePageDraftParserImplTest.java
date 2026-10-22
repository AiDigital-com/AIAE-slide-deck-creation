package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyIcon;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageDraftParserImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageIconLibraryServiceImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTemplateRegistryImpl;
import com.aidigital.strategyplanning.service.casestudy.templates.OnePageIconLibrary;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the One-Page draft parser.
 */
class OnePageDraftParserImplTest {

	private OnePageDraftParserImpl parser() {
		return new OnePageDraftParserImpl(new ObjectMapper(), new OnePageTemplateRegistryImpl(),
				new OnePageIconLibraryServiceImpl());
	}

	private String value(OnePageCaseStudyDraft draft, String key) {
		return draft.fields().stream().filter(field -> field.key().equals(key))
				.map(OnePageCaseStudyField::value).findFirst().orElseThrow();
	}

	private String icon(OnePageCaseStudyDraft draft, String slot) {
		OnePageCaseStudyIcon match = draft.icons().stream().filter(icon -> icon.slot().equals(slot))
				.findFirst().orElseThrow();
		return match.icon();
	}

	@Test
	void shouldBlankNonAnswersAndKeepTemplateIconsForEmptyItemsTest() {
		// Given: a reply with a real tactic, an "N/A" tactic, and icon picks of every kind
		String reply = "{\"fields\": {\"tactic_1_title\": \"Smart reallocation\", \"tactic_1_text\": "
				+ "\"Moved budget into Demand Gen.\", \"tactic_4_title\": \"N/A\", \"tactic_4_text\": \"\", "
				+ "\"driver_1\": \"Promos beat generic ads\"}, \"icons\": {\"tactic_1_icon\": \"swap-arrows\", "
				+ "\"tactic_4_icon\": \"target\", \"driver_1_icon\": \"not-a-real-icon\"}}";

		// When: it is parsed
		OnePageCaseStudyDraft draft = parser().parseDraft(reply, "report.pdf");

		// Then: values are normalized, non-answers blanked, and only icons beside content survive
		assertThat(value(draft, "tactic_1_title")).isEqualTo("SMART REALLOCATION");
		assertThat(value(draft, "tactic_4_title")).isEmpty();
		assertThat(value(draft, "challenge")).isEmpty();
		assertThat(draft.fields()).hasSize(new OnePageTemplateRegistryImpl().fields().size());
		assertThat(draft.fields().stream().filter(f -> f.key().equals("tactic_1_title")).findFirst().orElseThrow()
				.fit()).isEqualTo(OnePageFit.FITS);
		assertThat(draft.fields().stream().filter(f -> f.key().equals("challenge")).findFirst().orElseThrow()
				.fit()).isEqualTo(OnePageFit.EMPTY);
		assertThat(icon(draft, "tactic_1_icon")).isEqualTo("swap-arrows");
		// Tactic 4 has no content, so its template icon is kept even though the model picked one.
		assertThat(icon(draft, "tactic_4_icon")).isNull();
		// Names outside the library are dropped.
		assertThat(icon(draft, "driver_1_icon")).isNull();
		assertThat(draft.iconLibrary()).hasSize(OnePageIconLibrary.ICONS.size());
		assertThat(draft.sourceFileNames()).isEqualTo("report.pdf");
	}

	@Test
	void shouldRejectAReplyThatIsNotJsonTest() {
		// Given: a reply the model wrapped in prose
		OnePageDraftParserImpl parser = parser();

		// When-Then: the caller sees the external-call error code
		assertThatThrownBy(() -> parser.parseDraft("Sure! Here is the draft: {", "report.pdf"))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getCode()).isEqualTo("C003"));
	}

	@Test
	void shouldParseRevisionsLeavingOutNonAnswersAndUnknownKeysTest() {
		// Given: a revision reply with one real rewrite, one placeholder, and one unknown key
		String reply = "{\"fields\": {\"challenge\": \"Rewritten challenge.\", \"goal\": \"not available.\", "
				+ "\"mystery\": \"ignored\"}}";

		// When: it is parsed
		Map<String, String> revisions = parser().parseRevisions(reply);

		// Then: only the real rewrite survives, raw (formatting is the policy's job)
		assertThat(revisions).containsExactly(Map.entry("challenge", "Rewritten challenge."));
	}

	@Test
	void shouldTreatPlaceholdersAndBlanksAsNonAnswersTest() {
		// Given: the parser
		OnePageDraftParserImpl parser = parser();

		// When-Then: blanks and placeholder replies (with trailing marks, any case) mean "no value"
		assertThat(parser.isNonAnswer(null)).isTrue();
		assertThat(parser.isNonAnswer("  ")).isTrue();
		assertThat(parser.isNonAnswer("N/A")).isTrue();
		assertThat(parser.isNonAnswer("Not specified.")).isTrue();
		assertThat(parser.isNonAnswer("—")).isTrue();
		assertThat(parser.isNonAnswer("Scale conversions")).isFalse();
	}
}
