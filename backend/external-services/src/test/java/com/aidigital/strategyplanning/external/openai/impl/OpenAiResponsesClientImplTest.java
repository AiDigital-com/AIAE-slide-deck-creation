package com.aidigital.strategyplanning.external.openai.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the response handling of {@link OpenAiResponsesClientImpl}.
 *
 * <p>A {@code web_search}-enabled response is not a single message: tool call items sit in front
 * of the answer, and the answer itself may be split over several text parts. Each failure must
 * also stay distinguishable so callers can word the user-facing message correctly.
 */
class OpenAiResponsesClientImplTest {

	private OpenAiResponsesClientImpl client() {
		return new OpenAiResponsesClientImpl(mock(PooledRestClientFactory.class), new ObjectMapper());
	}

	@Test
	void shouldPreferTheTopLevelOutputTextWhenPresentTest() {
		// Given: an envelope carrying the convenience field
		String body = "{\"status\":\"completed\",\"output_text\":\"{\\\"fields\\\":{}}\","
				+ "\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"ignored\"}]}]}";

		// When: the result is read
		OpenAiResponsesResult result = client().resultOrFail(body);

		// Then: the convenience text wins and the raw output items are exposed alongside it
		assertThat(result.outputText()).isEqualTo("{\"fields\":{}}");
		assertThat(result.output()).hasSize(1);
	}

	@Test
	void shouldConcatenateMessageTextPartsAndSkipWebSearchCallItemsTest() {
		// Given: a tool-enabled answer with a search call before the message and two text parts
		String body = "{\"status\":\"completed\",\"output\":["
				+ "{\"type\":\"web_search_call\",\"status\":\"completed\"},"
				+ "{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"a\\\":\"},"
				+ "{\"type\":\"refusal\",\"refusal\":\"no\"},"
				+ "{\"type\":\"output_text\",\"text\":\"1}\"}]}]}";

		// When: the result is read
		OpenAiResponsesResult result = client().resultOrFail(body);

		// Then: only the text parts of message items are joined, in order, and the search call
		// is still visible in the raw output for callers that want citations
		assertThat(result.outputText()).isEqualTo("{\"a\":1}");
		assertThat(result.output()).hasSize(2);
		assertThat(result.output().get(0).path("type").asText()).isEqualTo("web_search_call");
	}

	@Test
	void shouldReportAnIncompleteResponseWithItsReasonTest() {
		// Given: the provider stopped at the output token ceiling
		String body = "{\"status\":\"incomplete\",\"incomplete_details\":{\"reason\":\"max_output_tokens\"},"
				+ "\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"{\"}]}]}";

		// When-Then: the partial text is not handed out as an answer, and the reason is kept
		assertThatThrownBy(() -> client().resultOrFail(body))
				.isInstanceOf(OpenAiExternalException.class)
				.satisfies(e -> {
					OpenAiExternalException failure = (OpenAiExternalException) e;
					assertThat(failure.getFailure()).isEqualTo(OpenAiFailure.INCOMPLETE);
					assertThat(failure.getMessage()).contains("max_output_tokens");
				});
	}

	@Test
	void shouldReportEmptyContentWhenNoTextIsPresentTest() {
		// Given: envelopes that are valid JSON but carry nothing usable, including an empty body
		// When-Then: each is reported as empty content, not a transport problem
		for (String body : new String[]{"", "{}", "{\"output\":[]}",
				"{\"output\":[{\"type\":\"web_search_call\"}]}",
				"{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\" \"}]}]}",
				"{\"status\":\"incomplete_not\",\"output_text\":\"\"}"}) {
			assertThatThrownBy(() -> client().resultOrFail(body))
					.isInstanceOf(OpenAiExternalException.class)
					.extracting(e -> ((OpenAiExternalException) e).getFailure())
					.isEqualTo(OpenAiFailure.EMPTY_CONTENT);
		}
	}

	@Test
	void shouldExposeAnEmptyOutputArrayWhenTheEnvelopeHasNoneTest() {
		// Given: only the convenience field, no output array
		// When: the result is read
		OpenAiResponsesResult result = client().resultOrFail("{\"output_text\":\"hi\"}");

		// Then: callers can iterate the output without a null check
		assertThat(result.output().isArray()).isTrue();
		assertThat(result.output()).isEmpty();
	}

	@Test
	void shouldReportATransportFailureWhenTheBodyIsNotJsonTest() {
		// Given: a body the provider never intended as JSON
		// When-Then: it is a transport failure, so callers do not report "no content"
		assertThatThrownBy(() -> client().resultOrFail("<html>gateway timeout</html>"))
				.isInstanceOf(OpenAiExternalException.class)
				.extracting(e -> ((OpenAiExternalException) e).getFailure())
				.isEqualTo(OpenAiFailure.TRANSPORT);
	}

	@Test
	void shouldReportAnInterruptedCallDistinctlyTest() {
		// Given: the calling thread was interrupted while waiting
		OpenAiResponsesClientImpl client = client();
		Thread.currentThread().interrupt();
		try {
			// When: the transport failure is mapped
			OpenAiExternalException failure = client.interruptedOrTransport(new RuntimeException("closed"));

			// Then: the interruption is preserved rather than reported as a plain failure
			assertThat(failure.getFailure()).isEqualTo(OpenAiFailure.INTERRUPTED);
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void shouldReuseOnePooledClientPerBaseUrlAndTimeoutTest() {
		// Given: a factory that hands out a client per pool
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		when(factory.createClient(eq("openai-responses"), anyString(), any(Duration.class)))
				.thenReturn(RestClient.create());
		OpenAiResponsesClientImpl client = new OpenAiResponsesClientImpl(factory, new ObjectMapper());

		// When: the same base URL and timeout are requested twice, then a longer timeout
		client.client("https://api.openai.com/v1", Duration.ofSeconds(180));
		client.client("https://api.openai.com/v1", Duration.ofSeconds(180));
		client.client("https://api.openai.com/v1", Duration.ofSeconds(300));

		// Then: one pool per distinct key, under the responses pool name
		verify(factory).createClient("openai-responses", "https://api.openai.com/v1",
				Duration.ofSeconds(180));
		verify(factory).createClient("openai-responses", "https://api.openai.com/v1",
				Duration.ofSeconds(300));
		verifyNoMoreInteractions(factory);
	}
}
