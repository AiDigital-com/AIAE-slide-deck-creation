package com.aidigital.strategyplanning.service.rfpoutline;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.rfpoutline.config.RfpOutlineProperties;
import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineDraft;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDraftParser;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlinePromptComposer;
import com.aidigital.strategyplanning.service.rfpoutline.services.impl.OpenAiRfpOutlineDraftServiceImpl;
import com.aidigital.strategyplanning.service.rfpoutline.templates.RfpOutlineTemplateTokens;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OpenAiRfpOutlineDraftServiceImpl}.
 *
 * <p>Covers the decisions this service owns rather than the prompt text: whether the drafting
 * engine is available, refusing to call the provider without a key, passing the uploaded file
 * names through to the parser, seeding every Google Docs template token, and translating each
 * provider failure into the application error the UI surfaces.
 */
class OpenAiRfpOutlineDraftServiceImplTest {

	private static final String CONNECTED_KEY = "sk-test-key";
	private static final String BASE_URL = "https://api.openai.com/v1";
	private static final String REQUEST_BODY = "{\"model\":\"gpt-4o\"}";
	private static final String MODEL_REPLY = "{\"clientName\":\"Acme\"}";

	private OpenAiRfpOutlineDraftServiceImpl service(String apiKey, OpenAiChatClient chatClient,
	                                                 RfpOutlinePromptComposer composer,
	                                                 RfpOutlineDraftParser parser) {
		RfpOutlineProperties properties = new RfpOutlineProperties();
		properties.setOpenaiApiKey(apiKey);
		properties.setOpenaiBaseUrl(BASE_URL);
		properties.setOpenaiModel("gpt-4o");
		return new OpenAiRfpOutlineDraftServiceImpl(properties, chatClient, composer, parser);
	}

	@Test
	void shouldReportDisconnectedUntilAnApiKeyIsConfiguredTest() {
		// Given: the same service with and without a key
		// When-Then: only a non-blank key counts as connected
		assertThat(service("", mock(OpenAiChatClient.class), mock(RfpOutlinePromptComposer.class),
				mock(RfpOutlineDraftParser.class)).isConnected()).isFalse();
		assertThat(service(null, mock(OpenAiChatClient.class), mock(RfpOutlinePromptComposer.class),
				mock(RfpOutlineDraftParser.class)).isConnected()).isFalse();
		assertThat(service(CONNECTED_KEY, mock(OpenAiChatClient.class),
				mock(RfpOutlinePromptComposer.class), mock(RfpOutlineDraftParser.class))
				.isConnected()).isTrue();
	}

	@Test
	void shouldRefuseToDraftWithoutCallingTheProviderWhenNotConnectedTest() {
		// Given: an unconfigured drafting engine
		OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
		RfpOutlinePromptComposer composer = mock(RfpOutlinePromptComposer.class);
		OpenAiRfpOutlineDraftServiceImpl service =
				service("", chatClient, composer, mock(RfpOutlineDraftParser.class));

		// When-Then: drafting fails with the external-call reason and names the missing key
		assertThatThrownBy(() -> service.draft(List.of(new SourceDocument("rfp.pdf", "body")), null))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C003.getCode())
				.hasMessageContaining("OPENAI_API_KEY");

		// Then: no provider call and no prompt were built at all
		verifyNoInteractions(chatClient, composer);
	}

	@Test
	void shouldPassEveryUploadedFileNameToTheParserTest() {
		// Given: a connected engine and two uploaded RFP documents
		OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
		RfpOutlinePromptComposer composer = mock(RfpOutlinePromptComposer.class);
		RfpOutlineDraftParser parser = mock(RfpOutlineDraftParser.class);
		List<SourceDocument> documents = List.of(
				new SourceDocument("rfp.pdf", "the brief"),
				new SourceDocument("annex.docx", "the annex"));
		RfpOutlineDraft expected = new RfpOutlineDraft("Title", "Acme", "Retail", "challenge",
				"opportunity", "solution", "outcome", "outline", "rfp.pdf, annex.docx");
		when(composer.buildRequestBody(documents, "focus on retail")).thenReturn(REQUEST_BODY);
		when(chatClient.complete(new OpenAiChatCall(BASE_URL, CONNECTED_KEY, REQUEST_BODY,
				Duration.ofSeconds(240)))).thenReturn(MODEL_REPLY);
		when(parser.parseDraft(MODEL_REPLY, "rfp.pdf, annex.docx")).thenReturn(expected);
		OpenAiRfpOutlineDraftServiceImpl service =
				service(CONNECTED_KEY, chatClient, composer, parser);

		// When: the documents are drafted
		RfpOutlineDraft draft = service.draft(documents, "focus on retail");

		// Then: the parsed draft is returned and both file names reached the parser, in order
		assertThat(draft).isEqualTo(expected);
		verify(parser).parseDraft(MODEL_REPLY, "rfp.pdf, annex.docx");
	}

	@Test
	void shouldSeedEveryTemplateTokenSoNoPlaceholderSurvivesTest() {
		// Given: a reviewed outline whose optional fields were left empty
		OpenAiRfpOutlineDraftServiceImpl service = service("", mock(OpenAiChatClient.class),
				mock(RfpOutlinePromptComposer.class), mock(RfpOutlineDraftParser.class));
		CreateRfpOutlineCommand command = new CreateRfpOutlineCommand("Acme RFP", "Acme", "Retail",
				"Costs were rising", null, "We rebuilt the funnel", null, "1. Intro", null,
				"rfp.pdf", "user_123");

		// When: template token values are built for the Google Doc
		Map<String, String> values = service.buildTemplateTokenValues(command);

		// Then: every token is present and non-null, so no {{token}} is left on the exported doc
		assertThat(values.keySet()).containsExactlyElementsOf(RfpOutlineTemplateTokens.TOKEN_KEYS);
		assertThat(values.values()).doesNotContainNull();
		assertThat(values.get("client_name")).isEqualTo("Acme");
		assertThat(values.get("challenge")).isEqualTo("Costs were rising");
		assertThat(values.get("deck_outline")).isEqualTo("1. Intro");
		assertThat(values.get("opportunity")).isEmpty();
		assertThat(values.get("outcome")).isEmpty();
	}

	/**
	 * Builds a service whose chat client fails the prepared call with the given provider failure.
	 *
	 * @param failure provider failure the client should raise
	 * @return service wired to that failing client
	 */
	private OpenAiRfpOutlineDraftServiceImpl serviceFailingWith(OpenAiExternalException failure) {
		OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
		when(chatClient.complete(new OpenAiChatCall(BASE_URL, CONNECTED_KEY, REQUEST_BODY,
				Duration.ofSeconds(240)))).thenThrow(failure);
		return service(CONNECTED_KEY, chatClient, mock(RfpOutlinePromptComposer.class),
				mock(RfpOutlineDraftParser.class));
	}

	@Test
	void shouldReportTheProviderStatusWhenTheCallIsRejectedTest() {
		// Given: the provider answers a prepared call with a rate-limit status
		OpenAiRfpOutlineDraftServiceImpl service = serviceFailingWith(
				new OpenAiExternalException(OpenAiFailure.HTTP_STATUS, 429, "too many requests"));

		// When-Then: the status reaches the message the UI shows
		assertThatThrownBy(() -> service.callChatCompletion(REQUEST_BODY))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C003.getCode())
				.hasMessageContaining("HTTP 429");
	}

	@Test
	void shouldDistinguishASilentModelReplyFromANetworkFailureTest() {
		// Given: two services differing only in how the provider failed
		OpenAiRfpOutlineDraftServiceImpl emptyReply = serviceFailingWith(
				new OpenAiExternalException(OpenAiFailure.EMPTY_CONTENT, 200, "blank content"));
		OpenAiRfpOutlineDraftServiceImpl unreachable = serviceFailingWith(new OpenAiExternalException(
				OpenAiFailure.TRANSPORT, "unreachable", new SocketTimeoutException("read timed out")));

		// When-Then: the user can tell an empty completion from a connectivity problem
		assertThatThrownBy(() -> emptyReply.callChatCompletion(REQUEST_BODY))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("returned no content");
		assertThatThrownBy(() -> unreachable.callChatCompletion(REQUEST_BODY))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("read timed out");
	}

	@Test
	void shouldPreserveTheInterruptedCauseForTheLogTest() {
		// Given: the waiting thread was interrupted
		OpenAiExternalException failure = new OpenAiExternalException(
				OpenAiFailure.INTERRUPTED, "interrupted", new InterruptedException("stopped"));
		OpenAiRfpOutlineDraftServiceImpl service = serviceFailingWith(failure);

		// When-Then: the reason is reported and the original failure is kept as the cause
		assertThatThrownBy(() -> service.callChatCompletion(REQUEST_BODY))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("interrupted")
				.hasCause(failure);
	}
}
