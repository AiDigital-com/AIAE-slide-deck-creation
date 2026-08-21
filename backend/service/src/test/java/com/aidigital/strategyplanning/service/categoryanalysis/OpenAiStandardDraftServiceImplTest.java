package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.website.WebsiteContentClient;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.ClientWebsiteReaderImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.OpenAiStandardDraftServiceImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.StandardDraftParserImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.StandardDraftPromptComposerImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.StandardTemplateTokenRegistryImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the Standard deck drafting collaborators: prompt building, website review,
 * reply parsing, and the orchestration in {@link OpenAiStandardDraftServiceImpl}.
 */
class OpenAiStandardDraftServiceImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private CategoryAnalysisProperties properties;
	private OpenAiStandardDraftServiceImpl service;
	private StandardDraftPromptComposerImpl composer;
	private StandardDraftParserImpl parser;
	private ClientWebsiteReaderImpl reader;

	@BeforeEach
	void setUp() {
		properties = new CategoryAnalysisProperties();
		StandardTemplateTokenRegistryImpl tokenRegistry = new StandardTemplateTokenRegistryImpl();
		composer = new StandardDraftPromptComposerImpl(properties, objectMapper, tokenRegistry);
		parser = new StandardDraftParserImpl(objectMapper, tokenRegistry);
		reader = new ClientWebsiteReaderImpl(mock(WebsiteContentClient.class));
		service = new OpenAiStandardDraftServiceImpl(properties, mock(OpenAiChatClient.class),
				reader, composer, parser);
	}

	@Test
	void isConnectedReflectsApiKeyPresence() {
		assertThat(service.isConnected()).isFalse();
		properties.setOpenaiApiKey("sk-test");
		assertThat(service.isConnected()).isTrue();
	}

	@Test
	void draftStandardFailsFastWhenNotConnected() {
		assertThatThrownBy(() -> service.draftStandard("Travel", "Acme", null, "https://acme.com", null))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("not connected");
	}

	@Test
	void buildRequestBodyMentionsEveryTokenKeyBriefWebsiteAndTheme() throws Exception {
		String body = composer.buildRequestBody("Regional Travel", "Acme Resorts", "focus on families",
				"https://acmeresorts.com", "shift to connected TV",
				"Acme Resorts operates lakeside family holiday resorts.");
		var root = objectMapper.readTree(body);
		assertThat(root.path("response_format").path("type").asText()).isEqualTo("json_object");
		String content = root.path("messages").path(0).path("content").asText();
		assertThat(content)
				.contains("Regional Travel")
				.contains("Acme Resorts")
				.contains("focus on families")
				.contains("https://acmeresorts.com")
				.contains("shift to connected TV")
				.contains("lakeside family holiday resorts")
				.contains("alignmentMatches")
				.contains("clientBusinessFocus");
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			assertThat(content).contains(spec.key());
		}
	}

	@Test
	void buildRequestBodyNotesWhenWebsiteUnavailable() throws Exception {
		String body = composer.buildRequestBody("Regional Travel", "Acme Resorts", null,
				"https://acmeresorts.com", null, null);
		String content = objectMapper.readTree(body).path("messages").path(0).path("content").asText();
		assertThat(content).contains("could not be retrieved");
	}

	@Test
	void parseDraftReturnsAllFieldsAndAlignment() {
		ObjectNode fields = objectMapper.createObjectNode();
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			fields.put(spec.key(), "value for " + spec.key());
		}
		ObjectNode root = objectMapper.createObjectNode();
		root.set("fields", fields);
		root.put("clientBusinessFocus", "Family holiday resorts.");
		root.put("alignmentMatches", true);
		root.put("alignmentMessage", "The deck fits the client's business.");

		StandardDraft draft = parser.parseDraft(root.toString());

		assertThat(draft.fields()).hasSize(StandardTemplateTokens.TOKENS.size());
		assertThat(draft.fields().get(0).slideNumber()).isEqualTo(1);
		assertThat(draft.alignment().clientBusinessFocus()).isEqualTo("Family holiday resorts.");
		assertThat(draft.alignment().confirmed()).isTrue();
		assertThat(draft.alignment().matches()).isTrue();
		assertThat(draft.alignment().message()).isEqualTo("The deck fits the client's business.");
	}

	@Test
	void parseDraftDefaultsAlignmentWhenModelOmitsIt() {
		ObjectNode fields = objectMapper.createObjectNode();
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			fields.put(spec.key(), "value for " + spec.key());
		}
		ObjectNode root = objectMapper.createObjectNode();
		root.set("fields", fields);

		StandardDraft draft = parser.parseDraft(root.toString());

		assertThat(draft.alignment().confirmed()).isTrue();
		assertThat(draft.alignment().matches()).isTrue();
		assertThat(draft.alignment().clientBusinessFocus()).isNull();
		assertThat(draft.alignment().message()).isNotBlank();
	}

	@Test
	void parseDraftRejectsMissingFields() {
		assertThatThrownBy(() -> parser.parseDraft("{\"fields\":{}}"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("incomplete");
	}

	@Test
	void parseDraftRejectsInvalidJson() {
		assertThatThrownBy(() -> parser.parseDraft("not json"))
				.isInstanceOf(AppException.class);
	}

	@Test
	void extractReadableTextStripsMarkupAndScripts() {
		String html = "<html><head><style>.x{color:red}</style></head><body>"
				+ "<script>var a=1;</script><h1>Acme &amp; Co</h1><p>We  sell   boots</p></body></html>";
		String text = reader.extractReadableText(html);
		assertThat(text).isEqualTo("Acme & Co We sell boots");
		assertThat(text).doesNotContain("color:red").doesNotContain("var a=1");
	}

	@Test
	void slideTokensReturnsOnlyThatSlidesTokens() {
		var slide1 = composer.slideTokens(1);
		assertThat(slide1).allMatch(t -> t.slideNumber() == 1).isNotEmpty();
		assertThat(composer.slideTokens(9)).isEmpty();
	}

	@Test
	void redraftSlideFailsFastWhenNotConnected() {
		assertThatThrownBy(() -> service.redraftSlide("Travel", "Acme", null, 1, "punchier",
				List.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("not connected");
	}

	@Test
	void buildSlideRequestBodyIncludesTargetSlideKeysBriefAndSlideNote() throws Exception {
		List<StandardFieldValue> current = new ArrayList<>();
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			current.add(new StandardFieldValue(spec.key(), "current " + spec.key()));
		}
		String body = composer.buildSlideRequestBody("Regional Travel", "Acme Resorts", "families",
				3, "make it Gen Z", current);
		var root = objectMapper.readTree(body);
		assertThat(root.path("response_format").path("type").asText()).isEqualTo("json_object");
		String content = root.path("messages").path(0).path("content").asText();
		assertThat(content).contains("Regional Travel").contains("Acme Resorts")
				.contains("families").contains("make it Gen Z");
		for (StandardTemplateTokens.TokenSpec spec : composer.slideTokens(3)) {
			assertThat(content).contains(spec.key());
		}
	}

	@Test
	void parseSlideDraftReturnsOnlySlideFields() {
		ObjectNode fields = objectMapper.createObjectNode();
		for (StandardTemplateTokens.TokenSpec spec : composer.slideTokens(3)) {
			fields.put(spec.key(), "redrafted " + spec.key());
		}
		ObjectNode root = objectMapper.createObjectNode();
		root.set("fields", fields);

		StandardDraft draft = parser.parseSlideDraft(root.toString(), 3);

		assertThat(draft.fields()).isNotEmpty().allMatch(f -> f.slideNumber() == 3);
		assertThat(draft.alignment()).isNull();
	}

	@Test
	void parseSlideDraftRejectsMissingSlideFields() {
		assertThatThrownBy(() -> parser.parseSlideDraft("{\"fields\":{}}", 3))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("incomplete");
	}

	@Test
	void redraftUnknownSlideNumberIsCaughtBeforeCall() {
		properties.setOpenaiApiKey("sk-test");
		assertThatThrownBy(() -> service.redraftSlide("Travel", "Acme", null, 9, null,
				List.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Unknown slide");
	}
}
