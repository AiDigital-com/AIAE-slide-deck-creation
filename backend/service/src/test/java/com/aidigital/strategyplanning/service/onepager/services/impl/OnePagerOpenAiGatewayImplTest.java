package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.onepager.config.OnePagerProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the One Page Generator's OpenAI gateway: the key check and the failure wording.
 */
class OnePagerOpenAiGatewayImplTest {

	private OnePagerProperties properties(String apiKey) {
		OnePagerProperties properties = new OnePagerProperties();
		properties.setOpenaiApiKey(apiKey);
		properties.setOpenaiBaseUrl("https://api.test/v1");
		return properties;
	}

	@Test
	void shouldRefuseToCallWhenNoKeyIsConfiguredTest() {
		// Given: no OpenAI key
		OpenAiChatClient client = mock(OpenAiChatClient.class);
		OnePagerOpenAiGatewayImpl gateway = new OnePagerOpenAiGatewayImpl(properties(""), client);

		// When-Then: the engine reports disconnected and the client is never touched
		assertThat(gateway.isConnected()).isFalse();
		assertThatThrownBy(() -> gateway.callChatCompletion("{}"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("AI drafting engine is not connected");
		verifyNoInteractions(client);
	}

	@Test
	void shouldForwardTheRequestWithTheFeaturesKeyAndTimeoutTest() {
		// Given: a connected gateway and a client that answers
		OpenAiChatClient client = mock(OpenAiChatClient.class);
		when(client.complete(any(OpenAiChatCall.class))).thenReturn("{\"topicPill\":\"AUTO\"}");
		OnePagerOpenAiGatewayImpl gateway = new OnePagerOpenAiGatewayImpl(properties("sk-test"), client);

		// When: a request body is sent
		String content = gateway.callChatCompletion("{\"model\":\"gpt-4o\"}");

		// Then: the content comes back and the call carried the key, URL, body, and timeout
		assertThat(content).isEqualTo("{\"topicPill\":\"AUTO\"}");
		ArgumentCaptor<OpenAiChatCall> call = ArgumentCaptor.forClass(OpenAiChatCall.class);
		verify(client).complete(call.capture());
		assertThat(call.getValue().apiKey()).isEqualTo("sk-test");
		assertThat(call.getValue().baseUrl()).isEqualTo("https://api.test/v1");
		assertThat(call.getValue().requestBody()).isEqualTo("{\"model\":\"gpt-4o\"}");
		assertThat(call.getValue().responseTimeout()).isEqualTo(OnePagerOpenAiGatewayImpl.CALL_TIMEOUT);
	}

	@Test
	void shouldWordEveryProviderFailureForTheUserTest() {
		// Given: a connected gateway
		OnePagerOpenAiGatewayImpl gateway = new OnePagerOpenAiGatewayImpl(properties("sk-test"),
				mock(OpenAiChatClient.class));

		// When-Then: each failure kind maps to its C003 message
		assertThat(gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.HTTP_STATUS, 429, "busy")))
				.hasMessageContaining("AI drafting call failed with HTTP 429");
		assertThat(gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.EMPTY_CONTENT, 200, "blank")))
				.hasMessageContaining("returned no content");
		assertThat(gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.INCOMPLETE, 200, "cut")))
				.hasMessageContaining("try fewer modules or a shorter brief");
		assertThat(gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.INTERRUPTED, "stop", null)))
				.hasMessageContaining("AI drafting call interrupted");
		AppException transport = gateway.draftCallFailure(new OpenAiExternalException(OpenAiFailure.TRANSPORT,
				"transport", new IOException("connection reset")));
		assertThat(transport).hasMessageContaining("AI drafting call failed: connection reset");
		assertThat(transport.getValidationMessage().getCode()).isEqualTo("C003");
		assertThat(gateway.rootMessage(new OpenAiExternalException(OpenAiFailure.TRANSPORT, "no cause", null)))
				.isEqualTo("no cause");
	}

	@Test
	void shouldTranslateAClientFailureRaisedDuringTheCallTest() {
		// Given: a client that fails with a status
		OpenAiChatClient client = mock(OpenAiChatClient.class);
		when(client.complete(any(OpenAiChatCall.class)))
				.thenThrow(new OpenAiExternalException(OpenAiFailure.HTTP_STATUS, 500, "boom"));
		OnePagerOpenAiGatewayImpl gateway = new OnePagerOpenAiGatewayImpl(properties("sk-test"), client);

		// When-Then: the caller sees the application error, not the provider exception
		assertThatThrownBy(() -> gateway.callChatCompletion("{}"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("HTTP 500");
	}
}
