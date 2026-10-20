package com.aidigital.strategyplanning.external.google.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.config.GoogleWorkspaceClientProperties;
import com.aidigital.strategyplanning.external.google.model.GoogleApiFailure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Transport-level tests for the file transfer calls of {@link GoogleWorkspaceClientImpl} against
 * a stub HTTP server.
 *
 * <p>Export and upload differ from the JSON calls in three ways worth pinning: the export body is
 * binary and must survive untouched, the upload is a hand-built {@code multipart/related} body
 * whose layout Drive is strict about, and both are resent on Google's transient 5xx pages but
 * never on a rejection.
 */
class GoogleWorkspaceClientTransferTransportTest {

	private static final String TOKEN = "ya29.token";
	private static final String PPTX_MIME =
			"application/vnd.openxmlformats-officedocument.presentationml.presentation";
	private static final String SLIDES_MIME = "application/vnd.google-apps.presentation";
	private static final Duration TRANSFER_TIMEOUT = Duration.ofSeconds(120);

	/**
	 * Builds a client whose export and upload pools point at the stub server, with a retry pause
	 * short enough for a unit test.
	 *
	 * @param server   stub HTTP server
	 * @param attempts total attempts allowed for a transfer call
	 * @return client under test
	 */
	private GoogleWorkspaceClientImpl clientFor(MockWebServer server, int attempts) {
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		RestClient restClient = RestClient.builder().baseUrl(server.url("/").toString()).build();
		when(factory.createClient(eq("drive-export"), eq("https://www.googleapis.com/drive/v3"),
				eq(TRANSFER_TIMEOUT))).thenReturn(restClient);
		when(factory.createClient(eq("drive-upload"),
				eq("https://www.googleapis.com/upload/drive/v3"), eq(TRANSFER_TIMEOUT)))
				.thenReturn(restClient);
		GoogleWorkspaceClientProperties properties = new GoogleWorkspaceClientProperties();
		properties.setTransientRetryAttempts(attempts);
		properties.setTransientRetryBackoff(Duration.ofMillis(1));
		return new GoogleWorkspaceClientImpl(factory, new ObjectMapper(), properties);
	}

	@Test
	void shouldExportADriveFileAsRawBytesWithTheUsersTokenTest() throws Exception {
		// Given: Drive answers the export with a binary body that is not valid text or JSON
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			byte[] pptx = {0x50, 0x4B, 0x03, 0x04, (byte) 0xFF, 0x00, (byte) 0x80};
			server.enqueue(new MockResponse().setResponseCode(200)
					.setBody(new Buffer().write(pptx)));

			// When: the file is exported as PPTX
			byte[] exported = clientFor(server, 3).exportDriveFile(TOKEN, "file_1", PPTX_MIME);

			// Then: the bytes come back untouched, from the export path with the format encoded
			// as a query parameter and the user's own token as the credential
			assertThat(exported).isEqualTo(pptx);
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("GET");
			assertThat(request.getPath()).isEqualTo("/files/file_1/export?mimeType="
					+ "application%2Fvnd.openxmlformats-officedocument.presentationml.presentation");
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + TOKEN);
		}
	}

	@Test
	void shouldResendAnExportThatHitsATransientServerErrorTest() throws Exception {
		// Given: Google's generic error page once, then the file
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(502).setBody("<html>That's an error</html>"));
			server.enqueue(new MockResponse().setResponseCode(200).setBody("pptx"));

			// When: the file is exported
			byte[] exported = clientFor(server, 3).exportDriveFile(TOKEN, "file_1", PPTX_MIME);

			// Then: the second attempt's body is what the caller gets
			assertThat(new String(exported, StandardCharsets.UTF_8)).isEqualTo("pptx");
			assertThat(server.getRequestCount()).isEqualTo(2);
		}
	}

	@Test
	void shouldGiveUpOnAnExportOnceTheTransientAttemptsAreUsedUpTest() throws Exception {
		// Given: Google keeps answering 502 for every attempt allowed
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(502).setBody("<html>error</html>"));
			server.enqueue(new MockResponse().setResponseCode(502).setBody("<html>error</html>"));
			GoogleWorkspaceClientImpl client = clientFor(server, 2);

			// When-Then: the last status reaches the caller, and no further request was sent
			assertThatThrownBy(() -> client.exportDriveFile(TOKEN, "file_1", PPTX_MIME))
					.isInstanceOf(GoogleApiException.class)
					.satisfies(e -> {
						GoogleApiException failure = (GoogleApiException) e;
						assertThat(failure.getFailure()).isEqualTo(GoogleApiFailure.HTTP_STATUS);
						assertThat(failure.getStatusCode()).isEqualTo(502);
						assertThat(failure.getResponseBody()).contains("error");
					});
			assertThat(server.getRequestCount()).isEqualTo(2);
		}
	}

	@Test
	void shouldNotResendAnExportThatWasRejectedTest() throws Exception {
		// Given: Drive refuses the export because the user may not read the file
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(403)
					.setBody("{\"error\":{\"message\":\"Insufficient permission\"}}"));
			GoogleWorkspaceClientImpl client = clientFor(server, 3);

			// When-Then: a rejection is final — resending it would only repeat the refusal
			assertThatThrownBy(() -> client.exportDriveFile(TOKEN, "file_1", PPTX_MIME))
					.isInstanceOf(GoogleApiException.class)
					.satisfies(e -> {
						GoogleApiException failure = (GoogleApiException) e;
						assertThat(failure.getFailure()).isEqualTo(GoogleApiFailure.HTTP_STATUS);
						assertThat(failure.getStatusCode()).isEqualTo(403);
						assertThat(failure.getResponseBody()).contains("Insufficient permission");
					});
			assertThat(server.getRequestCount()).isEqualTo(1);
		}
	}

	@Test
	void shouldReportAnEmptyExportAsATransportFailureTest() throws Exception {
		// Given: a 200 with no bytes at all
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200));
			GoogleWorkspaceClientImpl client = clientFor(server, 3);

			// When-Then: an empty file is unusable and is not reported as a Google rejection
			assertThatThrownBy(() -> client.exportDriveFile(TOKEN, "file_1", PPTX_MIME))
					.isInstanceOf(GoogleApiException.class)
					.satisfies(e -> assertThat(((GoogleApiException) e).getFailure())
							.isEqualTo(GoogleApiFailure.TRANSPORT));
		}
	}

	@Test
	void shouldReportAnUnreachableExportHostAsATransportFailureTest() throws Exception {
		// Given: a server that is shut down before the call
		MockWebServer server = new MockWebServer();
		server.start();
		GoogleWorkspaceClientImpl client = clientFor(server, 3);
		server.close();

		// When-Then: a refused connection is typed as transport and never retried as a 5xx
		assertThatThrownBy(() -> client.exportDriveFile(TOKEN, "file_1", PPTX_MIME))
				.isInstanceOf(GoogleApiException.class)
				.satisfies(e -> assertThat(((GoogleApiException) e).getFailure())
						.isEqualTo(GoogleApiFailure.TRANSPORT));
	}

	@Test
	void shouldUploadAFileAsAMultipartRelatedBodyAskingForConversionTest() throws Exception {
		// Given: Drive creates the converted presentation
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200)
					.setBody("{\"id\":\"slides_1\",\"mimeType\":\"" + SLIDES_MIME + "\"}"));
			byte[] pptx = "PK-pptx-bytes".getBytes(StandardCharsets.UTF_8);

			// When: the PPTX is uploaded as a Slides presentation
			JsonNode created = clientFor(server, 3)
					.uploadDriveFile(TOKEN, "One Pager", SLIDES_MIME, pptx, PPTX_MIME);

			// Then: the file is returned, and the request is the multipart upload Drive expects:
			// metadata part first naming the target type, then the media part with the bytes
			assertThat(created.path("id").asText()).isEqualTo("slides_1");
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getPath())
					.isEqualTo("/files?uploadType=multipart&supportsAllDrives=true");
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + TOKEN);
			String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
			assertThat(contentType).startsWith("multipart/related;").contains("boundary=");
			String boundary = contentType.substring(contentType.indexOf("boundary=") + "boundary=".length());
			String body = request.getBody().readUtf8();
			assertThat(body).isEqualTo("--" + boundary + "\r\n"
					+ "Content-Type: application/json; charset=UTF-8\r\n\r\n"
					+ "{\"name\":\"One Pager\",\"mimeType\":\"" + SLIDES_MIME + "\"}\r\n"
					+ "--" + boundary + "\r\n"
					+ "Content-Type: " + PPTX_MIME + "\r\n\r\n"
					+ "PK-pptx-bytes\r\n"
					+ "--" + boundary + "--");
		}
	}

	@Test
	void shouldResendAnUploadThatHitsATransientServerErrorTest() throws Exception {
		// Given: Google's generic error page once, then the created file
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(503).setBody("<html>error</html>"));
			server.enqueue(new MockResponse().setResponseCode(503).setBody("<html>error</html>"));
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"id\":\"slides_1\"}"));
			byte[] pptx = "PK".getBytes(StandardCharsets.UTF_8);

			// When: the upload is attempted. Apache HttpClient 5 retries a 503 once on its own,
			// so two 503s are consumed before this client's own retry sends the one that succeeds
			JsonNode created = clientFor(server, 3)
					.uploadDriveFile(TOKEN, "One Pager", SLIDES_MIME, pptx, PPTX_MIME);

			// Then: the caller only sees the success, and every attempt carried the same body
			assertThat(created.path("id").asText()).isEqualTo("slides_1");
			assertThat(server.getRequestCount()).isEqualTo(3);
			String first = server.takeRequest().getBody().readUtf8();
			server.takeRequest();
			assertThat(server.takeRequest().getBody().readUtf8()).isEqualTo(first);
		}
	}

	@Test
	void shouldReportAnUploadAnswerWithoutAFileIdAsATransportFailureTest() throws Exception {
		// Given: a 200 whose body names no file — an empty body and an empty object alike
		for (String body : new String[]{"", "{}"}) {
			try (MockWebServer server = new MockWebServer()) {
				server.start();
				server.enqueue(new MockResponse().setResponseCode(200).setBody(body));
				GoogleWorkspaceClientImpl client = clientFor(server, 3);
				byte[] pptx = "PK".getBytes(StandardCharsets.UTF_8);

				// When-Then: there is nothing to open, so this is not a success
				assertThatThrownBy(() -> client.uploadDriveFile(TOKEN, "One Pager", SLIDES_MIME,
						pptx, PPTX_MIME))
						.isInstanceOf(GoogleApiException.class)
						.satisfies(e -> assertThat(((GoogleApiException) e).getFailure())
								.isEqualTo(GoogleApiFailure.TRANSPORT));
			}
		}
	}

	@Test
	void shouldKeepGooglesReasonWhenAnUploadIsRejectedTest() throws Exception {
		// Given: Drive refuses the upload with a reason in the body
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(400)
					.setBody("{\"error\":{\"message\":\"Bad multipart\"}}"));
			GoogleWorkspaceClientImpl client = clientFor(server, 3);
			byte[] pptx = "PK".getBytes(StandardCharsets.UTF_8);

			// When-Then: the status and reason survive, and a 400 is not resent
			assertThatThrownBy(() -> client.uploadDriveFile(TOKEN, "One Pager", SLIDES_MIME,
					pptx, PPTX_MIME))
					.isInstanceOf(GoogleApiException.class)
					.satisfies(e -> {
						GoogleApiException failure = (GoogleApiException) e;
						assertThat(failure.getFailure()).isEqualTo(GoogleApiFailure.HTTP_STATUS);
						assertThat(failure.getStatusCode()).isEqualTo(400);
						assertThat(failure.getResponseBody()).contains("Bad multipart");
					});
			assertThat(server.getRequestCount()).isEqualTo(1);
		}
	}

	@Test
	void shouldUseTheTransferTimeoutAndSeparatePoolsForExportAndUploadTest() throws Exception {
		// Given: a factory recording which pools are asked for
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("pptx"));
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"id\":\"slides_1\"}"));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(eq("drive-export"), eq("https://www.googleapis.com/drive/v3"),
					eq(Duration.ofSeconds(7)))).thenReturn(
					RestClient.builder().baseUrl(server.url("/").toString()).build());
			when(factory.createClient(eq("drive-upload"),
					eq("https://www.googleapis.com/upload/drive/v3"), eq(Duration.ofSeconds(7))))
					.thenReturn(RestClient.builder().baseUrl(server.url("/").toString()).build());
			GoogleWorkspaceClientProperties properties = new GoogleWorkspaceClientProperties();
			properties.setTransferResponseTimeout(Duration.ofSeconds(7));
			GoogleWorkspaceClientImpl client =
					new GoogleWorkspaceClientImpl(factory, new ObjectMapper(), properties);

			// When: a file is exported and another uploaded
			client.exportDriveFile(TOKEN, "file_1", PPTX_MIME);
			client.uploadDriveFile(TOKEN, "One Pager", SLIDES_MIME,
					"PK".getBytes(StandardCharsets.UTF_8), PPTX_MIME);

			// Then: each transfer ran on its own pool with the configured transfer timeout, so
			// neither shares the ordinary Drive pool's shorter ceiling or its metrics series
			org.mockito.Mockito.verify(factory).createClient("drive-export",
					"https://www.googleapis.com/drive/v3", Duration.ofSeconds(7));
			org.mockito.Mockito.verify(factory).createClient("drive-upload",
					"https://www.googleapis.com/upload/drive/v3", Duration.ofSeconds(7));
			org.mockito.Mockito.verifyNoMoreInteractions(factory);
		}
	}
}
