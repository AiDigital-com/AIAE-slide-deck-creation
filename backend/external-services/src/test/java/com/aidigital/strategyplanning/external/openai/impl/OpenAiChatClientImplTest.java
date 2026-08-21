package com.aidigital.strategyplanning.external.openai.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the response handling of {@link OpenAiChatClientImpl}.
 *
 * <p>Callers translate the typed failure into their own user-facing error, so each reason must be
 * distinguishable: an unreadable body and an empty completion are different problems.
 */
class OpenAiChatClientImplTest {

	private OpenAiChatClientImpl client() {
		return new OpenAiChatClientImpl(mock(PooledRestClientFactory.class), new ObjectMapper());
	}

	@Test
	void shouldReturnTheMessageContentOfACompletionTest() {
		// Given: a well-formed chat completions envelope
		String body = "{\"choices\":[{\"message\":{\"content\":\"Drafted text\"}}]}";

		// When-Then: the content is unwrapped
		assertThat(client().contentOrFail(body)).isEqualTo("Drafted text");
	}

	@Test
	void shouldReportEmptyContentWhenTheEnvelopeCarriesNoCompletionTest() {
		// Given: envelopes that are valid JSON but carry nothing usable
		// When-Then: each is reported as an empty completion, not a transport problem
		for (String body : new String[]{"{}", "{\"choices\":[]}",
				"{\"choices\":[{\"message\":{\"content\":\"\"}}]}",
				"{\"choices\":[{\"message\":{\"content\":\"   \"}}]}"}) {
			assertThatThrownBy(() -> client().contentOrFail(body))
					.isInstanceOf(OpenAiExternalException.class)
					.extracting(e -> ((OpenAiExternalException) e).getFailure())
					.isEqualTo(OpenAiFailure.EMPTY_CONTENT);
		}
	}

	@Test
	void shouldReportATransportFailureWhenTheBodyIsNotJsonTest() {
		// Given: a body the provider never intended as JSON
		// When-Then: it is a transport failure, so callers do not report "no content"
		assertThatThrownBy(() -> client().contentOrFail("<html>gateway timeout</html>"))
				.isInstanceOf(OpenAiExternalException.class)
				.extracting(e -> ((OpenAiExternalException) e).getFailure())
				.isEqualTo(OpenAiFailure.TRANSPORT);
	}

	@Test
	void shouldReuseOnePooledClientPerBaseUrlAndTimeoutTest() {
		// Given: a factory that hands out a distinct client per call
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		org.mockito.Mockito.when(factory.createClient(
						org.mockito.ArgumentMatchers.eq("openai-chat"),
						org.mockito.ArgumentMatchers.anyString(),
						org.mockito.ArgumentMatchers.any(java.time.Duration.class)))
				.thenReturn(org.springframework.web.client.RestClient.create());
		OpenAiChatClientImpl client = new OpenAiChatClientImpl(factory, new ObjectMapper());

		// When: the same base URL and timeout are requested twice
		client.client("https://api.openai.com/v1", java.time.Duration.ofSeconds(120));
		client.client("https://api.openai.com/v1", java.time.Duration.ofSeconds(120));
		client.client("https://api.openai.com/v1", java.time.Duration.ofSeconds(240));

		// Then: one pool per distinct key — a pool per request would defeat pooling
		org.mockito.Mockito.verify(factory)
				.createClient("openai-chat", "https://api.openai.com/v1", java.time.Duration.ofSeconds(120));
		org.mockito.Mockito.verify(factory)
				.createClient("openai-chat", "https://api.openai.com/v1", java.time.Duration.ofSeconds(240));
		org.mockito.Mockito.verifyNoMoreInteractions(factory);
	}
}
