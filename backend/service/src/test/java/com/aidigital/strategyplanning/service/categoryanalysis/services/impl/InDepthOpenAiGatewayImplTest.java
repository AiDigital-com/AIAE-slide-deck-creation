package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.OpenAiResponsesClient;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesResult;
import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InDepthOpenAiGatewayImpl}: call shaping and failure mapping.
 */
class InDepthOpenAiGatewayImplTest {

	@Test
	void shouldSendChatCompletionsWithTheFeatureKeyAndBaseUrlTest() {
		// Given: a configured feature and a chat client
		CategoryAnalysisProperties properties = new CategoryAnalysisProperties();
		properties.setOpenaiApiKey("sk-test");
		properties.setOpenaiBaseUrl("https://api.example.com/v1");
		OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
		when(chatClient.complete(any(OpenAiChatCall.class))).thenReturn("{\"fields\":{}}");
		InDepthOpenAiGatewayImpl gateway = new InDepthOpenAiGatewayImpl(properties, chatClient,
				mock(OpenAiResponsesClient.class));

		// When: a body is sent
		String content = gateway.callChatCompletion("{\"model\":\"x\"}");

		// Then: the content comes back and the call carried this feature's settings
		assertThat(content).isEqualTo("{\"fields\":{}}");
		ArgumentCaptor<OpenAiChatCall> call = ArgumentCaptor.forClass(OpenAiChatCall.class);
		verify(chatClient).complete(call.capture());
		assertThat(call.getValue().apiKey()).isEqualTo("sk-test");
		assertThat(call.getValue().baseUrl()).isEqualTo("https://api.example.com/v1");
		assertThat(call.getValue().requestBody()).isEqualTo("{\"model\":\"x\"}");
		assertThat(call.getValue().responseTimeout()).isEqualTo(InDepthOpenAiGatewayImpl.CALL_TIMEOUT);
	}

	@Test
	void shouldReturnTheOutputTextOfAResponsesCallTest() {
		// Given: a responses client answering with output text
		CategoryAnalysisProperties properties = new CategoryAnalysisProperties();
		properties.setOpenaiApiKey("sk-test");
		OpenAiResponsesClient responsesClient = mock(OpenAiResponsesClient.class);
		when(responsesClient.respond(any(OpenAiResponsesCall.class)))
				.thenReturn(new OpenAiResponsesResult("{\"a\":1}", new ObjectMapper().createArrayNode()));
		InDepthOpenAiGatewayImpl gateway = new InDepthOpenAiGatewayImpl(properties, mock(OpenAiChatClient.class),
				responsesClient);

		// When: a body is sent
		String text = gateway.callResponses("{\"input\":\"x\"}");

		// Then: only the text is returned and the key was forwarded
		assertThat(text).isEqualTo("{\"a\":1}");
		ArgumentCaptor<OpenAiResponsesCall> call = ArgumentCaptor.forClass(OpenAiResponsesCall.class);
		verify(responsesClient).respond(call.capture());
		assertThat(call.getValue().apiKey()).isEqualTo("sk-test");
		assertThat(call.getValue().requestBody()).isEqualTo("{\"input\":\"x\"}");
	}

	@Test
	void shouldTranslateProviderFailuresOfBothClientsTest() {
		// Given: a chat client that is cut off and a responses client that is down
		OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
		when(chatClient.complete(any(OpenAiChatCall.class)))
				.thenThrow(new OpenAiExternalException(OpenAiFailure.INCOMPLETE, 200, "cut"));
		OpenAiResponsesClient responsesClient = mock(OpenAiResponsesClient.class);
		when(responsesClient.respond(any(OpenAiResponsesCall.class)))
				.thenThrow(new OpenAiExternalException(OpenAiFailure.TRANSPORT, "down", new IOException("socket closed")));
		InDepthOpenAiGatewayImpl gateway = new InDepthOpenAiGatewayImpl(new CategoryAnalysisProperties(),
				chatClient, responsesClient);

		// When-Then: each becomes this feature's application error
		assertThatThrownBy(() -> gateway.callChatCompletion("{}"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("fewer slides");
		assertThatThrownBy(() -> gateway.callResponses("{}"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("socket closed");
	}

	@Test
	void shouldTranslateEveryProviderFailureIntoAnApplicationErrorTest() {
		// Given: one failure of each kind
		InDepthOpenAiGatewayImpl gateway = new InDepthOpenAiGatewayImpl(new CategoryAnalysisProperties(),
				mock(OpenAiChatClient.class), mock(OpenAiResponsesClient.class));

		// When-Then: each maps to the message the UI shows
		assertThat(gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.HTTP_STATUS, 429, "x")))
				.hasMessageContaining("HTTP 429");
		assertThat(gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.EMPTY_CONTENT, 200, "x")))
				.hasMessageContaining("no content");
		assertThat(gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.INCOMPLETE, 200, "x")))
				.hasMessageContaining("fewer slides");
		assertThat(gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.INTERRUPTED, "x",
				new InterruptedException())))
				.hasMessageContaining("interrupted");
		assertThat(gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.TRANSPORT, "x",
				new IOException("socket closed"))))
				.hasMessageContaining("socket closed");
		assertThat(gateway.rootMessage(new OpenAiExternalException(OpenAiFailure.TRANSPORT, 500, "own message")))
				.isEqualTo("own message");
	}
}
