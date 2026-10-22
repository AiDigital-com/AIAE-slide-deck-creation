package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthDraftParserImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthTemplateTokenRegistryImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthValueFormatterImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link InDepthDraftParserImpl}.
 */
class InDepthDraftParserImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final InDepthTemplateTokenRegistryImpl registry = new InDepthTemplateTokenRegistryImpl();
	private final InDepthDraftParserImpl parser =
			new InDepthDraftParserImpl(objectMapper, new InDepthValueFormatterImpl(registry));

	@Test
	void shouldReturnAllFieldsAndAlignmentTest() {
		// Given: a complete reply for slide 2
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);
		ObjectNode root = replyFor(tokens, "value for ");
		root.put("clientBusinessFocus", "Family holiday resorts.");
		root.put("alignmentMatches", true);
		root.put("alignmentMessage", "The deck fits the client's business.");

		// When: it is parsed
		InDepthDraft draft = parser.parseDraft(root.toString(), tokens);

		// Then: every field is present with its slide numbers and the alignment is confirmed
		assertThat(draft.fields()).hasSize(tokens.size());
		assertThat(draft.fields().get(0).slideNumbers()).containsExactly(2);
		assertThat(draft.alignment().clientBusinessFocus()).isEqualTo("Family holiday resorts.");
		assertThat(draft.alignment().confirmed()).isTrue();
		assertThat(draft.alignment().matches()).isTrue();
		assertThat(draft.alignment().message()).isEqualTo("The deck fits the client's business.");
	}

	@Test
	void shouldDefaultAlignmentWhenTheModelOmitsItTest() {
		// Given: a reply with fields only
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);

		// When: it is parsed
		InDepthDraft draft = parser.parseDraft(replyFor(tokens, "value for ").toString(), tokens);

		// Then: alignment defaults to a confirmed match with a message
		assertThat(draft.alignment().confirmed()).isTrue();
		assertThat(draft.alignment().matches()).isTrue();
		assertThat(draft.alignment().clientBusinessFocus()).isNull();
		assertThat(draft.alignment().message()).isNotBlank();
	}

	@Test
	void shouldWarnWhenTheModelReportsAMismatchWithoutAMessageTest() {
		// Given: alignmentMatches false and no message
		ObjectNode root = objectMapper.createObjectNode();
		root.put("alignmentMatches", false);

		// When-Then: the default warning is used
		assertThat(parser.parseAlignment(root).message()).contains("may not match");
		assertThat(parser.parseAlignment(root).matches()).isFalse();
	}

	@Test
	void shouldReturnSlideFieldsWithNullAlignmentTest() {
		// Given: a redraft reply
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);

		// When: it is parsed as a slide draft
		InDepthDraft draft = parser.parseSlideDraft(replyFor(tokens, "redrafted ").toString(), tokens);

		// Then: fields only
		assertThat(draft.fields()).hasSize(tokens.size());
		assertThat(draft.alignment()).isNull();
	}

	@Test
	void shouldRejectMissingFieldsTest() {
		// Given-When-Then: an empty fields object names the missing keys
		assertThatThrownBy(() -> parser.parseDraft("{\"fields\":{}}", registry.tokensForSlide(2)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("incomplete")
				.hasMessageContaining("trend_headline");
	}

	@Test
	void shouldRejectPlaceholderValuesLikeMissingOnesTest() {
		// Given: a stat answered with "N/A"
		InDepthTokenSpec spec = tokenByKey("trend_stat_1_value");
		ObjectNode root = objectMapper.createObjectNode();
		root.putObject("fields").put(spec.key(), "N/A");

		// When-Then: the draft is incomplete
		assertThatThrownBy(() -> parser.parseFields(root, List.of(spec)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("incomplete")
				.hasMessageContaining(spec.key());
	}

	@Test
	void shouldFormatAndCapParsedValuesTest() {
		// Given: an over-long stat and a lower-case caps field
		InDepthTokenSpec stat = tokenByKey("trend_stat_1_value");
		InDepthTokenSpec title = tokenByKey("hi_highlight_1_title");
		ObjectNode root = objectMapper.createObjectNode();
		root.putObject("fields").put(stat.key(), "x".repeat(registry.maxCharsFor(stat) + 50))
				.put(title.key(), "demand anchors");

		// When: the fields are parsed
		List<InDepthDraftField> fields = parser.parseFields(root, List.of(stat, title));

		// Then: the stat is cut to budget and the title upper-cased
		assertThat(fields.get(0).value()).hasSize(registry.maxCharsFor(stat));
		assertThat(fields.get(1).value()).isEqualTo("DEMAND ANCHORS");
	}

	@Test
	void shouldRejectInvalidJsonTest() {
		// Given-When-Then: non-JSON output is an application error
		assertThatThrownBy(() -> parser.parseDraft("not json", registry.tokensForSlide(2)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("not valid JSON");
	}

	@Test
	void shouldTolerateAMarkdownFenceAroundTheJsonObjectTest() {
		// Given: a fenced reply, as web_search calls sometimes produce
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);
		String fenced = "```json\n" + replyFor(tokens, "value for ") + "\n```";

		// When-Then: it still parses
		assertThat(parser.parseDraft(fenced, tokens).fields()).hasSize(tokens.size());
	}

	@Test
	void shouldTolerateStrayCommentaryAroundTheJsonObjectTest() {
		// Given: prose before and after the object
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);
		String withCommentary = "Here is the drafted content:\n" + replyFor(tokens, "value for ")
				+ "\nLet me know if you need anything else.";

		// When-Then: it still parses
		assertThat(parser.parseDraft(withCommentary, tokens).fields()).hasSize(tokens.size());
	}

	@Test
	void shouldKeepCorrectionsAndDropPlaceholdersTest() {
		// Given: a fact-check reply that corrects one value and gives up on another
		InDepthTokenSpec stat = tokenByKey("trend_stat_1_value");
		InDepthTokenSpec sources = tokenByKey("trend_sources");
		ObjectNode root = objectMapper.createObjectNode();
		root.put(stat.key(), "12.4M").put(sources.key(), "unable to verify");

		// When: corrections are read
		Map<String, String> corrections = parser.parseCorrections(root, List.of(stat, sources));

		// Then: only the real correction is applied
		assertThat(corrections).containsExactly(Map.entry(stat.key(), "12.4M"));
	}

	private ObjectNode replyFor(List<InDepthTokenSpec> tokens, String prefix) {
		ObjectNode fields = objectMapper.createObjectNode();
		for (InDepthTokenSpec spec : tokens) {
			fields.put(spec.key(), prefix + spec.key());
		}
		ObjectNode root = objectMapper.createObjectNode();
		root.set("fields", fields);
		return root;
	}

	private InDepthTokenSpec tokenByKey(String key) {
		return InDepthTemplateTokens.TOKENS.stream().filter(t -> t.key().equals(key)).findFirst().orElseThrow();
	}
}
