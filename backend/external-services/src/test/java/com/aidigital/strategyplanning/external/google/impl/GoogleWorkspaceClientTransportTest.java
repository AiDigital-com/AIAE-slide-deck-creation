package com.aidigital.strategyplanning.external.google.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.model.GoogleApiFailure;
import com.fasterxml.jackson.databind.JsonNode;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Transport-level tests for {@link GoogleWorkspaceClientImpl} against a stub HTTP server.
 *
 * <p>These cover what a mocked {@code RestClient} cannot: that each call reaches the right path
 * and method, that the user's own OAuth token is the credential sent, and that a rejection is
 * turned into a typed failure carrying Google's own reason text.
 */
class GoogleWorkspaceClientTransportTest {

	private static final String TOKEN = "ya29.token";

	/**
	 * Builds a client whose pooled REST client points at the stub server.
	 *
	 * @param server       stub HTTP server
	 * @param objectMapper mapper for request and response bodies
	 * @return client under test
	 */
	private GoogleWorkspaceClientImpl clientFor(MockWebServer server, ObjectMapper objectMapper) {
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		RestClient restClient = RestClient.builder()
				.baseUrl(server.url("/").toString())
				.build();
		when(factory.createClient(eq("drive"), eq("https://www.googleapis.com/drive/v3"),
				eq(Duration.ofSeconds(60)))).thenReturn(restClient);
		when(factory.createClient(eq("google-slides"), eq("https://slides.googleapis.com/v1"),
				eq(Duration.ofSeconds(60)))).thenReturn(restClient);
		when(factory.createClient(eq("google-docs"), eq("https://docs.googleapis.com/v1"),
				eq(Duration.ofSeconds(60)))).thenReturn(restClient);
		return new GoogleWorkspaceClientImpl(factory, objectMapper);
	}

	@Test
	void shouldCopyADriveFileAcrossSharedDrivesWithTheUsersTokenTest() throws Exception {
		// Given: Drive answers the copy with the new file
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"id\":\"copy_1\"}"));

			// When: a template is copied
			JsonNode result = clientFor(server, new ObjectMapper())
					.copyDriveFile(TOKEN, "template_1", "My Deck");

			// Then: the request names the template, opts into shared drives, and carries the
			// user's own bearer token rather than a service account
			assertThat(result.path("id").asText()).isEqualTo("copy_1");
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getPath())
					.isEqualTo("/files/template_1/copy?supportsAllDrives=true");
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION))
					.isEqualTo("Bearer " + TOKEN);
			assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"My Deck\"}");
		}
	}

	@Test
	void shouldReadAPresentationTest() throws Exception {
		// Given: Slides returns a presentation
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"presentationId\":\"deck_1\"}"));

			// When: the presentation is read
			JsonNode result = clientFor(server, new ObjectMapper())
					.getPresentation(TOKEN, "deck_1");

			// Then: it is a GET on the presentation path
			assertThat(result.path("presentationId").asText()).isEqualTo("deck_1");
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("GET");
			assertThat(request.getPath()).isEqualTo("/presentations/deck_1");
		}
	}

	@Test
	void shouldSendSlidesAndDocsBatchUpdatesToTheirOwnPathsTest() throws Exception {
		// Given: both batchUpdate endpoints accept the envelope
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
			GoogleWorkspaceClientImpl client = clientFor(server, new ObjectMapper());

			// When: a deck and a doc are each filled
			client.batchUpdatePresentation(TOKEN, "deck_1", "{\"requests\":[]}");
			client.batchUpdateDocument(TOKEN, "doc_1", "{\"requests\":[]}");

			// Then: a deck update never lands on the document endpoint or the reverse
			assertThat(server.takeRequest().getPath())
					.isEqualTo("/presentations/deck_1:batchUpdate");
			assertThat(server.takeRequest().getPath())
					.isEqualTo("/documents/doc_1:batchUpdate");
		}
	}

	@Test
	void shouldSendAnEmptyObjectWhenThereIsNoRequestBodyTest() throws Exception {
		// Given: a POST with no body to send
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

			// When: the call goes out
			clientFor(server, new ObjectMapper())
					.post("https://slides.googleapis.com/v1", "google-slides", "/x", TOKEN, null);

			// Then: Google receives valid JSON rather than an empty entity it would reject
			assertThat(server.takeRequest().getBody().readUtf8()).isEqualTo("{}");
		}
	}

	@Test
	void shouldRaiseATypedFailureCarryingGooglesReasonTextTest() throws Exception {
		// Given: Drive refuses the copy and explains why in the body
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"error\":{\"message\":\"Insufficient permission\"}}"));

			// When-Then: the status and the provider's own reason both survive, because the
			// status line alone does not say what went wrong
			assertThatThrownBy(() -> clientFor(server, new ObjectMapper())
					.copyDriveFile(TOKEN, "template_1", "My Deck"))
					.isInstanceOf(GoogleApiException.class)
					.satisfies(e -> {
						GoogleApiException failure = (GoogleApiException) e;
						assertThat(failure.getFailure()).isEqualTo(GoogleApiFailure.HTTP_STATUS);
						assertThat(failure.getStatusCode()).isEqualTo(403);
						assertThat(failure.getResponseBody()).contains("Insufficient permission");
					});
		}
	}

	@Test
	void shouldReportAnUnparseableBodyAsATransportFailureTest() throws Exception {
		// Given: a 200 whose body is not JSON at all
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("<html>gateway</html>"));

			// When-Then: an intermediary's HTML page is a transport problem, not a Google answer
			assertThatThrownBy(() -> clientFor(server, new ObjectMapper())
					.getPresentation(TOKEN, "deck_1"))
					.isInstanceOf(GoogleApiException.class)
					.satisfies(e -> assertThat(((GoogleApiException) e).getFailure())
							.isEqualTo(GoogleApiFailure.TRANSPORT));
		}
	}

	@Test
	void shouldReportAnUnreachableHostAsATransportFailureTest() throws IOException {
		// Given: a server that is shut down before the call
		MockWebServer server = new MockWebServer();
		server.start();
		GoogleWorkspaceClientImpl client = clientFor(server, new ObjectMapper());
		server.close();

		// When-Then: a refused connection arrives as a typed transport failure
		assertThatThrownBy(() -> client.getPresentation(TOKEN, "deck_1"))
				.isInstanceOf(GoogleApiException.class)
				.satisfies(e -> assertThat(((GoogleApiException) e).getFailure())
						.isEqualTo(GoogleApiFailure.TRANSPORT));
	}

	@Test
	void shouldReuseOnePoolPerApiHostTest() throws IOException {
		// Given: a client asked for the same host twice
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient("google-slides", "https://slides.googleapis.com/v1",
					Duration.ofSeconds(60)))
					.thenReturn(RestClient.builder().baseUrl(server.url("/").toString()).build());
			GoogleWorkspaceClientImpl client =
					new GoogleWorkspaceClientImpl(factory, new ObjectMapper());

			// When: the pooled client is resolved twice for the same base URL
			RestClient first = client.client("https://slides.googleapis.com/v1", "google-slides");
			RestClient second = client.client("https://slides.googleapis.com/v1", "google-slides");

			// Then: one pool serves the host, so repeated calls do not leak connections
			assertThat(first).isSameAs(second);
		}
	}
}
