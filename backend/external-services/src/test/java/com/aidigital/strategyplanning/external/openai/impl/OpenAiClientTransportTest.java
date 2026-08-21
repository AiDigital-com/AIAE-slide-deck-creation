package com.aidigital.strategyplanning.external.openai.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.aidigital.strategyplanning.external.openai.model.OpenAiImageCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Transport-level tests for the two OpenAI clients against a stub HTTP server.
 *
 * <p>These pin the parts a mocked {@code RestClient} cannot reach: the endpoint path, the API key
 * as a bearer credential, the request body passing through untouched, and each failure mode
 * arriving as its own typed reason so callers can word the user-facing message correctly.
 */
class OpenAiClientTransportTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(120);

	/**
	 * Builds a factory whose pooled client points at the stub server.
	 *
	 * @param server     stub HTTP server
	 * @param clientName pool name the client under test asks for
	 * @return mocked factory
	 */
	private PooledRestClientFactory factoryFor(MockWebServer server, String clientName) {
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		when(factory.createClient(eq(clientName), anyString(), eq(TIMEOUT)))
				.thenReturn(RestClient.builder().baseUrl(server.url("/").toString()).build());
		return factory;
	}

	@Test
	void shouldPostAChatCompletionAndReturnTheMessageContentTest() throws Exception {
		// Given: OpenAI answers with one choice
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody(
					"{\"choices\":[{\"message\":{\"content\":\"Drafted outline\"}}]}"));
			OpenAiChatClientImpl client = new OpenAiChatClientImpl(
					factoryFor(server, "openai-chat"), new ObjectMapper());

			// When: a completion is requested
			String content = client.complete(new OpenAiChatCall("https://api.openai.com/v1",
					"sk-key", "{\"model\":\"gpt-4o\"}", TIMEOUT));

			// Then: the content comes back and the request carried the key and body verbatim
			assertThat(content).isEqualTo("Drafted outline");
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getPath()).isEqualTo("/chat/completions");
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer sk-key");
			assertThat(request.getBody().readUtf8()).isEqualTo("{\"model\":\"gpt-4o\"}");
		}
	}

	@Test
	void shouldRaiseTheStatusFailureWhenChatIsRejectedTest() throws IOException {
		// Given: OpenAI rate limits the request. Two responses are queued because Apache
		// HttpClient 5 retries 429 and 503 once by default, so the provider really does see
		// the request twice before the caller is told.
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(429)
					.setBody("{\"error\":{\"message\":\"Rate limit reached\"}}"));
			server.enqueue(new MockResponse().setResponseCode(429)
					.setBody("{\"error\":{\"message\":\"Rate limit reached\"}}"));
			OpenAiChatClientImpl client = new OpenAiChatClientImpl(
					factoryFor(server, "openai-chat"), new ObjectMapper());

			// When-Then: the status reaches the caller so it can distinguish a rejection from
			// a malformed answer, and the built-in retry is what produced the second attempt
			assertThatThrownBy(() -> client.complete(new OpenAiChatCall(
					"https://api.openai.com/v1", "sk-key", "{}", TIMEOUT)))
					.isInstanceOf(OpenAiExternalException.class)
					.satisfies(e -> {
						OpenAiExternalException failure = (OpenAiExternalException) e;
						assertThat(failure.getFailure()).isEqualTo(OpenAiFailure.HTTP_STATUS);
						assertThat(failure.getStatusCode()).isEqualTo(429);
					});
			assertThat(server.getRequestCount()).isEqualTo(2);
		}
	}

	@Test
	void shouldRaiseTheEmptyContentFailureWhenChatAnswersWithNothingUsableTest() throws IOException {
		// Given: a 200 whose choices carry no content
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"choices\":[]}"));
			OpenAiChatClientImpl client = new OpenAiChatClientImpl(
					factoryFor(server, "openai-chat"), new ObjectMapper());

			// When-Then: an empty answer is its own reason, not an HTTP error
			assertThatThrownBy(() -> client.complete(new OpenAiChatCall(
					"https://api.openai.com/v1", "sk-key", "{}", TIMEOUT)))
					.isInstanceOf(OpenAiExternalException.class)
					.satisfies(e -> assertThat(((OpenAiExternalException) e).getFailure())
							.isEqualTo(OpenAiFailure.EMPTY_CONTENT));
		}
	}

	@Test
	void shouldReportAnUnreachableChatHostAsATransportFailureTest() throws IOException {
		// Given: the endpoint is gone before the call
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiChatClientImpl client = new OpenAiChatClientImpl(
				factoryFor(server, "openai-chat"), new ObjectMapper());
		server.close();

		// When-Then: a refused connection is a transport failure, not a model answer
		assertThatThrownBy(() -> client.complete(new OpenAiChatCall(
				"https://api.openai.com/v1", "sk-key", "{}", TIMEOUT)))
				.isInstanceOf(OpenAiExternalException.class)
				.satisfies(e -> assertThat(((OpenAiExternalException) e).getFailure())
						.isEqualTo(OpenAiFailure.TRANSPORT));
	}

	@Test
	void shouldPostAnImageGenerationAndDecodeThePayloadTest() throws Exception {
		// Given: OpenAI returns a base64 image
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			String encoded = Base64.getEncoder()
					.encodeToString("png-bytes".getBytes(StandardCharsets.UTF_8));
			server.enqueue(new MockResponse().setResponseCode(200)
					.setBody("{\"data\":[{\"b64_json\":\"" + encoded + "\"}]}"));
			OpenAiImageClientImpl client = new OpenAiImageClientImpl(
					factoryFor(server, "openai-images"), new ObjectMapper());

			// When: an image is generated
			byte[] image = client.generate(new OpenAiImageCall("https://api.openai.com/v1",
					"sk-key", "{\"prompt\":\"a shop\"}", TIMEOUT));

			// Then: the decoded bytes are returned from the image endpoint
			assertThat(new String(image, StandardCharsets.UTF_8)).isEqualTo("png-bytes");
			RecordedRequest request = server.takeRequest();
			assertThat(request.getPath()).isEqualTo("/images/generations");
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer sk-key");
		}
	}

	@Test
	void shouldKeepTheProvidersReasonWhenImageGenerationIsRejectedTest() throws IOException {
		// Given: the image request is refused with a reason in the body
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(400)
					.setBody("{\"error\":{\"message\":\"content policy\"}}"));
			OpenAiImageClientImpl client = new OpenAiImageClientImpl(
					factoryFor(server, "openai-images"), new ObjectMapper());

			// When-Then: the body survives, because "HTTP 400" alone does not say the prompt
			// was refused on policy grounds
			assertThatThrownBy(() -> client.generate(new OpenAiImageCall(
					"https://api.openai.com/v1", "sk-key", "{}", TIMEOUT)))
					.isInstanceOf(OpenAiExternalException.class)
					.satisfies(e -> {
						OpenAiExternalException failure = (OpenAiExternalException) e;
						assertThat(failure.getFailure()).isEqualTo(OpenAiFailure.HTTP_STATUS);
						assertThat(failure.getResponseBody()).contains("content policy");
					});
		}
	}

	@Test
	void shouldRaiseTheEmptyContentFailureWhenNoImageIsReturnedTest() throws IOException {
		// Given: a 200 whose data array carries no payload
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[]}"));
			OpenAiImageClientImpl client = new OpenAiImageClientImpl(
					factoryFor(server, "openai-images"), new ObjectMapper());

			// When-Then: an answer with no image is not a transport problem
			assertThatThrownBy(() -> client.generate(new OpenAiImageCall(
					"https://api.openai.com/v1", "sk-key", "{}", TIMEOUT)))
					.isInstanceOf(OpenAiExternalException.class)
					.satisfies(e -> assertThat(((OpenAiExternalException) e).getFailure())
							.isEqualTo(OpenAiFailure.EMPTY_CONTENT));
		}
	}

	@Test
	void shouldReportAnUnreachableImageHostAsATransportFailureTest() throws IOException {
		// Given: the endpoint is gone before the call
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiImageClientImpl client = new OpenAiImageClientImpl(
				factoryFor(server, "openai-images"), new ObjectMapper());
		server.close();

		// When-Then: the failure is typed as transport
		assertThatThrownBy(() -> client.generate(new OpenAiImageCall(
				"https://api.openai.com/v1", "sk-key", "{}", TIMEOUT)))
				.isInstanceOf(OpenAiExternalException.class)
				.satisfies(e -> assertThat(((OpenAiExternalException) e).getFailure())
						.isEqualTo(OpenAiFailure.TRANSPORT));
	}
}
