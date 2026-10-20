package com.aidigital.strategyplanning.external.openai.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Transport-level tests for {@link OpenAiResponsesClientImpl} against a stub HTTP server.
 *
 * <p>These pin the endpoint path, the API key as a bearer credential, the request body — tools
 * and all — passing through untouched, and each failure mode arriving as its own typed reason.
 */
class OpenAiResponsesClientTransportTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(180);
	private static final String BASE_URL = "https://api.openai.com/v1";

	/**
	 * Builds a client whose pooled REST client points at the stub server.
	 *
	 * @param server stub HTTP server
	 * @return client under test
	 */
	private OpenAiResponsesClientImpl clientFor(MockWebServer server) {
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		when(factory.createClient(eq("openai-responses"), anyString(), eq(TIMEOUT)))
				.thenReturn(RestClient.builder().baseUrl(server.url("/").toString()).build());
		return new OpenAiResponsesClientImpl(factory, new ObjectMapper());
	}

	@Test
	void shouldPostAResponsesRequestAndReturnTheOutputTest() throws Exception {
		// Given: OpenAI answers a web-search call with a search item and a message
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody(
					"{\"status\":\"completed\",\"output\":[{\"type\":\"web_search_call\"},"
							+ "{\"type\":\"message\",\"content\":[{\"type\":\"output_text\","
							+ "\"text\":\"{\\\"ok\\\":true}\"}]}]}"));
			String requestBody = "{\"model\":\"gpt-4o\",\"input\":\"verify\",\"tools\":[{\"type\":\"web_search\"}]}";

			// When: the call is made
			OpenAiResponsesResult result = clientFor(server)
					.respond(new OpenAiResponsesCall(BASE_URL, "sk-key", requestBody, TIMEOUT));

			// Then: the text comes back and the request hit /responses with the key and the
			// body — tools included — exactly as the caller built it
			assertThat(result.outputText()).isEqualTo("{\"ok\":true}");
			assertThat(result.output()).hasSize(2);
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getPath()).isEqualTo("/responses");
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer sk-key");
			assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).startsWith("application/json");
			assertThat(request.getBody().readUtf8()).isEqualTo(requestBody);
		}
	}

	@Test
	void shouldRaiseTheStatusFailureCarryingTheProvidersReasonWhenRejectedTest() throws IOException {
		// Given: the request is refused with a reason in the body
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(400)
					.setBody("{\"error\":{\"message\":\"Web Search cannot be used with JSON mode\"}}"));
			OpenAiResponsesClientImpl client = clientFor(server);

			// When-Then: status and body both survive, because the status alone does not say
			// the request shape was wrong
			assertThatThrownBy(() -> client.respond(new OpenAiResponsesCall(BASE_URL, "sk-key",
					"{}", TIMEOUT)))
					.isInstanceOf(OpenAiExternalException.class)
					.satisfies(e -> {
						OpenAiExternalException failure = (OpenAiExternalException) e;
						assertThat(failure.getFailure()).isEqualTo(OpenAiFailure.HTTP_STATUS);
						assertThat(failure.getStatusCode()).isEqualTo(400);
						assertThat(failure.getResponseBody()).contains("JSON mode");
					});
		}
	}

	@Test
	void shouldRaiseTheEmptyContentFailureWhenTheBodyIsEmptyTest() throws IOException {
		// Given: a 200 with no body at all
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200));
			OpenAiResponsesClientImpl client = clientFor(server);

			// When-Then: an empty answer is its own reason, not an HTTP error
			assertThatThrownBy(() -> client.respond(new OpenAiResponsesCall(BASE_URL, "sk-key",
					"{}", TIMEOUT)))
					.isInstanceOf(OpenAiExternalException.class)
					.satisfies(e -> assertThat(((OpenAiExternalException) e).getFailure())
							.isEqualTo(OpenAiFailure.EMPTY_CONTENT));
		}
	}

	@Test
	void shouldReportAnUnreachableHostAsATransportFailureTest() throws IOException {
		// Given: the endpoint is gone before the call
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiResponsesClientImpl client = clientFor(server);
		server.close();

		// When-Then: a refused connection is a transport failure, not a model answer
		assertThatThrownBy(() -> client.respond(new OpenAiResponsesCall(BASE_URL, "sk-key",
				"{}", TIMEOUT)))
				.isInstanceOf(OpenAiExternalException.class)
				.satisfies(e -> assertThat(((OpenAiExternalException) e).getFailure())
						.isEqualTo(OpenAiFailure.TRANSPORT));
	}
}
