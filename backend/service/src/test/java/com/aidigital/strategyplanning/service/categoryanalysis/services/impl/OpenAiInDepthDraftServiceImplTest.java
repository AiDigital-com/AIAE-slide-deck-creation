package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.OpenAiResponsesClient;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesResult;
import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.ClientWebsiteReader;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the orchestration in {@link OpenAiInDepthDraftServiceImpl}: the knowledge draft,
 * the per-topic fact-check, the alignment check, and single-slide redrafts.
 */
class OpenAiInDepthDraftServiceImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final InDepthTemplateTokenRegistryImpl registry = new InDepthTemplateTokenRegistryImpl();
	private CategoryAnalysisProperties properties;
	private OpenAiChatClient chatClient;
	private OpenAiResponsesClient responsesClient;
	private ClientWebsiteReader websiteReader;
	private OpenAiInDepthDraftServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new CategoryAnalysisProperties();
		chatClient = mock(OpenAiChatClient.class);
		responsesClient = mock(OpenAiResponsesClient.class);
		websiteReader = mock(ClientWebsiteReader.class);
		InDepthValueFormatterImpl formatter = new InDepthValueFormatterImpl(registry);
		InDepthOpenAiGatewayImpl gateway = new InDepthOpenAiGatewayImpl(properties, chatClient, responsesClient);
		InDepthDraftPromptComposerImpl composer = new InDepthDraftPromptComposerImpl(properties, objectMapper, registry);
		InDepthDraftParserImpl parser = new InDepthDraftParserImpl(objectMapper, formatter);
		service = new OpenAiInDepthDraftServiceImpl(properties, gateway, websiteReader, registry, composer, parser,
				new InDepthFactCheckerImpl(gateway, composer, parser, formatter));
	}

	@Test
	void shouldReportConnectionFromTheApiKeyTest() {
		// Given-When-Then: blank key means not connected
		assertThat(service.isConnected()).isFalse();
		properties.setOpenaiApiKey("sk-test");
		assertThat(service.isConnected()).isTrue();
	}

	@Test
	void shouldFailFastWhenNotConnectedTest() {
		// Given: no API key
		// When-Then: neither drafting nor redrafting reaches the provider
		assertThatThrownBy(() -> service.draftInDepth("Travel", "Acme", null, "https://acme.com", List.of(2)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("not connected");
		assertThatThrownBy(() -> service.redraftSlide("Travel", "Acme", null, 2, null, List.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("not connected");
		verifyNoInteractions(chatClient, responsesClient, websiteReader);
	}

	@Test
	void shouldFailFastWhenNoSlidesAreSelectedTest() {
		// Given: a connected engine
		properties.setOpenaiApiKey("sk-test");

		// When-Then: an empty or null selection is rejected before any call
		assertThatThrownBy(() -> service.draftInDepth("Travel", "Acme", null, "https://acme.com", List.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("No slides selected");
		assertThatThrownBy(() -> service.draftInDepth("Travel", "Acme", null, "https://acme.com", null))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("No slides selected");
		verifyNoInteractions(chatClient, responsesClient, websiteReader);
	}

	@Test
	void shouldReturnAnEmptyDraftWhenOnlyContentFreeSlidesAreCheckedTest() {
		// Given: only the cover is checked
		properties.setOpenaiApiKey("sk-test");

		// When: the draft is requested
		InDepthDraft draft = service.draftInDepth("Travel", "Acme", null, "https://acme.com", List.of(1));

		// Then: nothing is drafted and no AI call is made
		assertThat(draft.fields()).isEmpty();
		assertThat(draft.alignment()).isNull();
		verifyNoInteractions(chatClient, responsesClient, websiteReader);
	}

	@Test
	void shouldDraftThenFactCheckThenCheckAlignmentTest() throws IOException {
		// Given: a connected engine, a readable website, a complete knowledge draft, a fact-check that
		// corrects one stat and gives up on another, and a positive alignment verdict
		properties.setOpenaiApiKey("sk-test");
		properties.setOpenaiBaseUrl("https://api.example.com/v1");
		when(websiteReader.fetchWebsiteText("https://acme.com")).thenReturn("Acme sells lakeside holidays.");
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);
		when(chatClient.complete(any(OpenAiChatCall.class))).thenReturn(replyFor(tokens).toString());
		ObjectNode corrections = objectMapper.createObjectNode();
		corrections.put("trend_stat_1_value", "12.4M").put("trend_sources", "n/a");
		ObjectNode alignment = objectMapper.createObjectNode();
		alignment.put("clientBusinessFocus", "Lakeside holidays").put("alignmentMatches", true)
				.put("alignmentMessage", "Fits.");
		when(responsesClient.respond(any(OpenAiResponsesCall.class)))
				.thenReturn(new OpenAiResponsesResult(corrections.toString(), objectMapper.createArrayNode()))
				.thenReturn(new OpenAiResponsesResult(alignment.toString(), objectMapper.createArrayNode()));

		// When: the deck is drafted
		InDepthDraft draft = service.draftInDepth("Travel", "Acme", "families", "https://acme.com", List.of(2));

		// Then: the corrected stat replaced the draft, the placeholder was ignored, alignment is confirmed,
		// and the calls carried this feature's key and base URL
		assertThat(valueOf(draft, "trend_stat_1_value")).isEqualTo("12.4M");
		assertThat(valueOf(draft, "trend_sources")).isEqualTo("drafted trend_sources");
		assertThat(valueOf(draft, "trend_headline")).isEqualTo("drafted trend_headline");
		assertThat(draft.alignment().confirmed()).isTrue();
		assertThat(draft.alignment().matches()).isTrue();
		assertThat(draft.alignment().clientBusinessFocus()).isEqualTo("Lakeside holidays");
		ArgumentCaptor<OpenAiChatCall> chatCall = ArgumentCaptor.forClass(OpenAiChatCall.class);
		verify(chatClient).complete(chatCall.capture());
		assertThat(chatCall.getValue().apiKey()).isEqualTo("sk-test");
		assertThat(chatCall.getValue().baseUrl()).isEqualTo("https://api.example.com/v1");
		assertThat(objectMapper.readTree(chatCall.getValue().requestBody()).path("messages").path(0)
				.path("content").asText()).contains("lakeside holidays");
		ArgumentCaptor<OpenAiResponsesCall> responsesCalls = ArgumentCaptor.forClass(OpenAiResponsesCall.class);
		verify(responsesClient, org.mockito.Mockito.times(2)).respond(responsesCalls.capture());
		assertThat(objectMapper.readTree(responsesCalls.getAllValues().get(0).requestBody())
				.path("tools").get(0).path("type").asText()).isEqualTo("web_search");
		assertThat(objectMapper.readTree(responsesCalls.getAllValues().get(1).requestBody())
				.path("text").path("format").path("type").asText()).isEqualTo("json_object");
	}

	@Test
	void shouldKeepDraftedValuesWhenTheFactCheckFailsAndMarkAlignmentUnconfirmedWhenTheSiteIsUnreadableTest() {
		// Given: an unreadable website, a complete draft, and a fact-check that blows up
		properties.setOpenaiApiKey("sk-test");
		when(websiteReader.fetchWebsiteText("https://acme.com")).thenReturn(null);
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);
		when(chatClient.complete(any(OpenAiChatCall.class))).thenReturn(replyFor(tokens).toString());
		ObjectNode alignment = objectMapper.createObjectNode();
		alignment.put("clientBusinessFocus", "Guessed focus");
		when(responsesClient.respond(any(OpenAiResponsesCall.class)))
				.thenThrow(new OpenAiExternalException(OpenAiFailure.HTTP_STATUS, 500, "boom"))
				.thenReturn(new OpenAiResponsesResult(alignment.toString(), objectMapper.createArrayNode()));

		// When: the deck is drafted
		InDepthDraft draft = service.draftInDepth("Travel", "Acme", null, "https://acme.com", List.of(2));

		// Then: every drafted value survives and the alignment says the site could not be read
		assertThat(draft.fields()).hasSize(tokens.size());
		assertThat(valueOf(draft, "trend_sources")).isEqualTo("drafted trend_sources");
		assertThat(draft.alignment().confirmed()).isFalse();
		assertThat(draft.alignment().clientBusinessFocus()).isEqualTo("Guessed focus");
		assertThat(draft.alignment().message()).contains("couldn't read https://acme.com");
	}

	@Test
	void shouldDegradeToAnUncheckedAlignmentWhenTheAlignmentCallFailsTest() {
		// Given: slide 16's fields, whose fact-check call and alignment call both fail
		properties.setOpenaiApiKey("sk-test");
		when(websiteReader.fetchWebsiteText("https://acme.com")).thenReturn("text");
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(16);
		when(chatClient.complete(any(OpenAiChatCall.class))).thenReturn(replyFor(tokens).toString());
		when(responsesClient.respond(any(OpenAiResponsesCall.class)))
				.thenThrow(new OpenAiExternalException(OpenAiFailure.TRANSPORT, "down", new IOException("down")));

		// When: the deck is drafted
		InDepthDraft draft = service.draftInDepth("Travel", "Acme", null, "https://acme.com", List.of(16));

		// Then: the draft is returned with an "unchecked" verdict after one fact-check and one alignment call
		assertThat(draft.fields()).hasSize(tokens.size());
		assertThat(draft.alignment().confirmed()).isFalse();
		assertThat(draft.alignment().message()).contains("could not be checked");
		verify(responsesClient, org.mockito.Mockito.times(2)).respond(any(OpenAiResponsesCall.class));
	}

	@Test
	void shouldRedraftOneSlideThroughTheResponsesApiTest() {
		// Given: a connected engine and a complete redraft reply
		properties.setOpenaiApiKey("sk-test");
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);
		when(responsesClient.respond(any(OpenAiResponsesCall.class)))
				.thenReturn(new OpenAiResponsesResult(replyFor(tokens).toString(), objectMapper.createArrayNode()));

		// When: slide 2 is redrafted
		InDepthDraft draft = service.redraftSlide("Travel", "Acme", null, 2, "punchier", List.of());

		// Then: only slide 2's fields come back, with no alignment, and chat completions were not used
		assertThat(draft.fields()).hasSize(tokens.size()).allMatch(f -> f.slideNumbers().contains(2));
		assertThat(draft.alignment()).isNull();
		verify(chatClient, never()).complete(any(OpenAiChatCall.class));
	}

	@Test
	void shouldRejectAnUnknownSlideBeforeCallingTheProviderTest() {
		// Given: a connected engine
		properties.setOpenaiApiKey("sk-test");

		// When-Then: a slide with no tokens is rejected
		assertThatThrownBy(() -> service.redraftSlide("Travel", "Acme", null, 999, null, List.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Unknown slide");
		verifyNoInteractions(responsesClient);
	}

	@Test
	void shouldSurfaceAProviderFailureOfTheKnowledgeDraftTest() {
		// Given: the knowledge draft is cut off
		properties.setOpenaiApiKey("sk-test");
		when(websiteReader.fetchWebsiteText("https://acme.com")).thenReturn("text");
		when(chatClient.complete(any(OpenAiChatCall.class)))
				.thenThrow(new OpenAiExternalException(OpenAiFailure.INCOMPLETE, 200, "cut"));

		// When-Then: the draft fails with the incomplete message and nothing else is attempted
		assertThatThrownBy(() -> service.draftInDepth("Travel", "Acme", null, "https://acme.com", List.of(2)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("fewer slides");
		verifyNoInteractions(responsesClient);
	}

	private ObjectNode replyFor(List<InDepthTokenSpec> tokens) {
		ObjectNode fields = objectMapper.createObjectNode();
		for (InDepthTokenSpec spec : tokens) {
			fields.put(spec.key(), "drafted " + spec.key());
		}
		ObjectNode root = objectMapper.createObjectNode();
		root.set("fields", fields);
		return root;
	}

	private String valueOf(InDepthDraft draft, String key) {
		return draft.fields().stream().filter(f -> f.key().equals(key)).map(InDepthDraftField::value)
				.findFirst().orElseThrow();
	}
}
