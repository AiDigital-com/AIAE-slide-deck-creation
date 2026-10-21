package com.aidigital.strategyplanning.external.google.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.GoogleWorkspaceClient;
import com.aidigital.strategyplanning.external.google.config.GoogleWorkspaceClientProperties;
import com.aidigital.strategyplanning.external.google.model.GoogleApiFailure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Pooled implementation of {@link GoogleWorkspaceClient}.
 *
 * <p>Drive, Slides, and Docs are separate hosts and each gets its own pool. File export and
 * upload run on their own pools too, named {@code drive-export} and {@code drive-upload}: they
 * carry whole files, use the longer transfer timeout from {@link GoogleWorkspaceClientProperties},
 * and show up as their own series in the outbound metrics, so a slow conversion never hides
 * inside the ordinary Drive numbers.
 *
 * <p>Every {@code exchange} call passes {@code close = true}. Spring closes the response only in
 * that case, and each handler here reads the whole body into memory before returning, so there is
 * nothing left to stream. With {@code false} the pooled connection is never released and the pool
 * starves after {@code max-connections-per-route} calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleWorkspaceClientImpl implements GoogleWorkspaceClient {

	private static final String DRIVE_API_BASE = "https://www.googleapis.com/drive/v3";
	private static final String DRIVE_UPLOAD_API_BASE = "https://www.googleapis.com/upload/drive/v3";
	private static final String SLIDES_API_BASE = "https://slides.googleapis.com/v1";
	private static final String DOCS_API_BASE = "https://docs.googleapis.com/v1";
	private static final String DRIVE_CLIENT = "drive";
	private static final String DRIVE_EXPORT_CLIENT = "drive-export";
	private static final String DRIVE_UPLOAD_CLIENT = "drive-upload";
	private static final String SLIDES_CLIENT = "google-slides";
	private static final String DOCS_CLIENT = "google-docs";
	private static final Duration CALL_TIMEOUT = Duration.ofSeconds(60);
	private static final String EXPORT_PATH = "/files/{fileId}/export?mimeType={mimeType}";
	private static final String UPLOAD_PATH = "/files?uploadType=multipart&supportsAllDrives=true";
	private static final String MULTIPART_BOUNDARY_PREFIX = "strategy-planning-";
	private static final String CRLF = "\r\n";
	private static final String EXPORT_ACTION = "Google Drive export";
	private static final String UPLOAD_ACTION = "Google Drive upload";

	private final PooledRestClientFactory restClientFactory;
	private final ObjectMapper objectMapper;
	private final GoogleWorkspaceClientProperties properties;
	private final Map<String, RestClient> clientsByName = new ConcurrentHashMap<>();

	@Override
	public JsonNode copyDriveFile(String accessToken, String fileId, String name) {
		String body = objectMapper.createObjectNode().put("name", name).toString();
		return post(DRIVE_API_BASE, DRIVE_CLIENT,
				"/files/" + fileId + "/copy?supportsAllDrives=true", accessToken, body);
	}

	@Override
	public JsonNode getPresentation(String accessToken, String presentationId) {
		return get(SLIDES_API_BASE, SLIDES_CLIENT, "/presentations/" + presentationId, accessToken);
	}

	@Override
	public JsonNode batchUpdatePresentation(String accessToken, String presentationId,
			String requestsJson) {
		return post(SLIDES_API_BASE, SLIDES_CLIENT,
				"/presentations/" + presentationId + ":batchUpdate", accessToken, requestsJson);
	}

	@Override
	public JsonNode batchUpdateDocument(String accessToken, String documentId, String requestsJson) {
		return post(DOCS_API_BASE, DOCS_CLIENT,
				"/documents/" + documentId + ":batchUpdate", accessToken, requestsJson);
	}

	@Override
	public byte[] exportDriveFile(String accessToken, String fileId, String mimeType) {
		byte[] content = withTransientRetry(EXPORT_ACTION, () -> exportBytes(accessToken, fileId, mimeType));
		if (content.length == 0) {
			throw new GoogleApiException(GoogleApiFailure.TRANSPORT, 200, "",
					EXPORT_ACTION + " returned an empty file");
		}
		return content;
	}

	@Override
	public JsonNode uploadDriveFile(String accessToken, String name, String targetMimeType,
			byte[] content, String sourceMimeType) {
		String boundary = MULTIPART_BOUNDARY_PREFIX + UUID.randomUUID();
		String metadata = objectMapper.createObjectNode()
				.put("name", name)
				.put("mimeType", targetMimeType)
				.toString();
		byte[] body = multipartBody(boundary, metadata, sourceMimeType, content);
		JsonNode created = parse(uploadBytes(accessToken, boundary, body));
		if (created.path("id").asText("").isBlank()) {
			throw new GoogleApiException(GoogleApiFailure.TRANSPORT, 200, created.toString(),
					UPLOAD_ACTION + " returned no file id");
		}
		return created;
	}

	/**
	 * Performs the export GET and returns the raw file bytes.
	 *
	 * @param accessToken user's Google OAuth access token
	 * @param fileId      file to export
	 * @param mimeType    export format
	 * @return exported bytes, possibly empty
	 */
	byte[] exportBytes(String accessToken, String fileId, String mimeType) {
		try {
			return client(DRIVE_API_BASE, DRIVE_EXPORT_CLIENT, properties.getTransferResponseTimeout())
					.get()
					.uri(EXPORT_PATH, fileId, mimeType)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					.exchange(this::bytesOrFail, true);
		} catch (ResourceAccessException | UncheckedIOException e) {
			throw transportFailure(e);
		}
	}

	/**
	 * Performs the multipart upload POST and returns the raw response body.
	 *
	 * @param accessToken user's Google OAuth access token
	 * @param boundary    multipart boundary the body was built with
	 * @param body        complete {@code multipart/related} body
	 * @return raw response body
	 */
	String uploadBytes(String accessToken, String boundary, byte[] body) {
		try {
			return uploadClient()
					.post()
					.uri(UPLOAD_PATH)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					.contentType(MediaType.parseMediaType("multipart/related; boundary=" + boundary))
					.body(body)
					.exchange(this::bodyOrFail, true);
		} catch (ResourceAccessException | UncheckedIOException e) {
			throw transportFailure(e);
		}
	}

	/**
	 * Builds the {@code multipart/related} body Drive expects for a metadata-plus-media upload:
	 * a JSON metadata part followed by the media part, in that order.
	 *
	 * @param boundary       multipart boundary
	 * @param metadataJson   serialized file metadata ({@code name}, {@code mimeType})
	 * @param sourceMimeType type of the media bytes
	 * @param content        media bytes
	 * @return complete request body
	 */
	byte[] multipartBody(String boundary, String metadataJson, String sourceMimeType, byte[] content) {
		byte[] metadataPart = ("--" + boundary + CRLF
				+ "Content-Type: application/json; charset=UTF-8" + CRLF + CRLF
				+ metadataJson + CRLF).getBytes(StandardCharsets.UTF_8);
		byte[] mediaHeader = ("--" + boundary + CRLF
				+ "Content-Type: " + sourceMimeType + CRLF + CRLF).getBytes(StandardCharsets.UTF_8);
		byte[] closing = (CRLF + "--" + boundary + "--").getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream body = new ByteArrayOutputStream(
				metadataPart.length + mediaHeader.length + content.length + closing.length);
		body.writeBytes(metadataPart);
		body.writeBytes(mediaHeader);
		body.writeBytes(content);
		body.writeBytes(closing);
		return body.toByteArray();
	}

	/**
	 * Runs an idempotent file transfer call, resending it after a pause while it keeps failing
	 * with a 5xx.
	 *
	 * <p>Google's export endpoint occasionally answers a transient {@code 502}/{@code 503} page,
	 * and a repeated GET is harmless. Only the export uses this: the multipart upload is a
	 * non-idempotent {@code POST /files}, and Drive may commit the file and still answer 5xx, so
	 * resending it could create a duplicate. Any other failure — a 4xx, a transport error, an
	 * interruption — propagates immediately.
	 *
	 * @param action short description of the call, for the retry log line
	 * @param call   the call to run
	 * @param <T>    the call's result type
	 * @return the first non-5xx result
	 * @throws GoogleApiException the last failure once the attempts are used up, or the first
	 *                            non-transient one
	 */
	<T> T withTransientRetry(String action, Supplier<T> call) {
		int attempts = properties.getTransientRetryAttempts();
		for (int attempt = 1; attempt < attempts; attempt++) {
			try {
				return call.get();
			} catch (GoogleApiException e) {
				if (!isTransient(e)) {
					throw e;
				}
				log.warn("{} returned HTTP {} (attempt {}/{}), retrying after a short backoff",
						action, e.getStatusCode(), attempt, attempts);
				backOff(attempt);
			}
		}
		return call.get();
	}

	/**
	 * Tells whether a failure is a 5xx answer worth resending the same request for.
	 *
	 * @param failure failure raised by a transfer call
	 * @return true for an HTTP status failure of 500 or above
	 */
	boolean isTransient(GoogleApiException failure) {
		return failure.getFailure() == GoogleApiFailure.HTTP_STATUS
				&& failure.getStatusCode() >= HttpStatus.INTERNAL_SERVER_ERROR.value();
	}

	/**
	 * Pauses before a retry, growing linearly with the attempt number.
	 *
	 * @param attempt attempt that just failed, starting at 1
	 * @throws GoogleApiException as {@link GoogleApiFailure#INTERRUPTED} when the pause is cut short
	 */
	void backOff(int attempt) {
		try {
			Thread.sleep(properties.getTransientRetryBackoff().toMillis() * attempt);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new GoogleApiException(GoogleApiFailure.INTERRUPTED,
					"Google API retry interrupted", e);
		}
	}

	/**
	 * Performs a GET and parses the response body.
	 *
	 * @param baseUrl     API base URL
	 * @param clientName  pool name for diagnostics
	 * @param path        request path relative to the base URL
	 * @param accessToken user's Google OAuth access token
	 * @return parsed response body
	 */
	JsonNode get(String baseUrl, String clientName, String path, String accessToken) {
		try {
			return parse(client(baseUrl, clientName)
					.get()
					.uri(path)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					.exchange(this::bodyOrFail, true));
		} catch (ResourceAccessException | UncheckedIOException e) {
			throw transportFailure(e);
		}
	}

	/**
	 * Performs a POST and parses the response body.
	 *
	 * @param baseUrl     API base URL
	 * @param clientName  pool name for diagnostics
	 * @param path        request path relative to the base URL
	 * @param accessToken user's Google OAuth access token
	 * @param body        serialized request body
	 * @return parsed response body
	 */
	JsonNode post(String baseUrl, String clientName, String path, String accessToken, String body) {
		try {
			return parse(client(baseUrl, clientName)
					.post()
					.uri(path)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.body(body == null ? "{}" : body)
					.exchange(this::bodyOrFail, true));
		} catch (ResourceAccessException | UncheckedIOException e) {
			throw transportFailure(e);
		}
	}

	/**
	 * Returns the response body, or raises the typed status failure.
	 *
	 * @param request  outgoing request
	 * @param response provider response
	 * @return raw response body
	 * @throws IOException when the body cannot be read
	 */
	String bodyOrFail(org.springframework.http.HttpRequest request,
			RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
		return new String(bytesOrFail(request, response), StandardCharsets.UTF_8);
	}

	/**
	 * Returns the raw response bytes, or raises the typed status failure.
	 *
	 * @param request  outgoing request
	 * @param response provider response
	 * @return raw response bytes
	 * @throws IOException when the body cannot be read
	 */
	byte[] bytesOrFail(org.springframework.http.HttpRequest request,
			RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
		byte[] body = response.getBody().readAllBytes();
		if (!response.getStatusCode().is2xxSuccessful()) {
			String text = new String(body, StandardCharsets.UTF_8);
			log.warn("Google API call failed ({} {}): {}", response.getStatusCode().value(),
					request.getURI(), text);
			throw new GoogleApiException(GoogleApiFailure.HTTP_STATUS,
					response.getStatusCode().value(), text,
					"Google API returned HTTP " + response.getStatusCode().value());
		}
		return body;
	}

	/**
	 * Parses a response body into a JSON tree.
	 *
	 * @param body raw response body
	 * @return parsed tree
	 */
	JsonNode parse(String body) {
		try {
			return objectMapper.readTree(body);
		} catch (IOException e) {
			throw new GoogleApiException(GoogleApiFailure.TRANSPORT,
					"Google API response was not valid JSON", e);
		}
	}

	/**
	 * Maps a Spring transport failure onto the interrupted or generic transport reason.
	 *
	 * @param cause transport failure raised by the REST client
	 * @return the typed provider exception to throw
	 */
	GoogleApiException transportFailure(RuntimeException cause) {
		if (Thread.currentThread().isInterrupted()) {
			return new GoogleApiException(GoogleApiFailure.INTERRUPTED,
					"Google API call interrupted", cause);
		}
		return new GoogleApiException(GoogleApiFailure.TRANSPORT,
				cause.getMessage(), cause);
	}

	/**
	 * Returns the upload pool's client, creating it on first use. Its automatic retries are
	 * disabled so a {@code 503} is never silently resent for the non-idempotent upload.
	 *
	 * @return pooled REST client that sends each request once
	 */
	RestClient uploadClient() {
		return clientsByName.computeIfAbsent(DRIVE_UPLOAD_CLIENT,
				key -> restClientFactory.createSingleAttemptClient(key, DRIVE_UPLOAD_API_BASE,
						properties.getTransferResponseTimeout()));
	}

	/**
	 * Returns the pooled client for an API base URL with the ordinary call timeout, creating it
	 * on first use.
	 *
	 * @param baseUrl    API base URL
	 * @param clientName pool name for diagnostics
	 * @return pooled REST client
	 */
	RestClient client(String baseUrl, String clientName) {
		return client(baseUrl, clientName, CALL_TIMEOUT);
	}

	/**
	 * Returns the pooled client registered under a name, creating it on first use.
	 *
	 * <p>Each name maps to exactly one base URL and timeout, so the name alone is the pool key.
	 *
	 * @param baseUrl         API base URL
	 * @param clientName      pool name for diagnostics and metrics
	 * @param responseTimeout maximum wait for the first response byte
	 * @return pooled REST client
	 */
	RestClient client(String baseUrl, String clientName, Duration responseTimeout) {
		return clientsByName.computeIfAbsent(clientName,
				key -> restClientFactory.createClient(key, baseUrl, responseTimeout));
	}
}
