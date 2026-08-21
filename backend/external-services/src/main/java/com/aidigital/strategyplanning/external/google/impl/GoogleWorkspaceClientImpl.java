package com.aidigital.strategyplanning.external.google.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.GoogleWorkspaceClient;
import com.aidigital.strategyplanning.external.google.model.GoogleApiFailure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pooled implementation of {@link GoogleWorkspaceClient}.
 *
 * <p>Drive, Slides, and Docs are separate hosts and each gets its own pool.
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
	private static final String SLIDES_API_BASE = "https://slides.googleapis.com/v1";
	private static final String DOCS_API_BASE = "https://docs.googleapis.com/v1";
	private static final Duration CALL_TIMEOUT = Duration.ofSeconds(60);

	private final PooledRestClientFactory restClientFactory;
	private final ObjectMapper objectMapper;
	private final Map<String, RestClient> clientsByBaseUrl = new ConcurrentHashMap<>();

	@Override
	public JsonNode copyDriveFile(String accessToken, String fileId, String name) {
		String body = objectMapper.createObjectNode().put("name", name).toString();
		return post(DRIVE_API_BASE, "drive",
				"/files/" + fileId + "/copy?supportsAllDrives=true", accessToken, body);
	}

	@Override
	public JsonNode getPresentation(String accessToken, String presentationId) {
		return get(SLIDES_API_BASE, "google-slides", "/presentations/" + presentationId, accessToken);
	}

	@Override
	public JsonNode batchUpdatePresentation(String accessToken, String presentationId,
			String requestsJson) {
		return post(SLIDES_API_BASE, "google-slides",
				"/presentations/" + presentationId + ":batchUpdate", accessToken, requestsJson);
	}

	@Override
	public JsonNode batchUpdateDocument(String accessToken, String documentId, String requestsJson) {
		return post(DOCS_API_BASE, "google-docs",
				"/documents/" + documentId + ":batchUpdate", accessToken, requestsJson);
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
		String body = new String(response.getBody().readAllBytes());
		if (!response.getStatusCode().is2xxSuccessful()) {
			log.warn("Google API call failed ({} {}): {}", response.getStatusCode().value(),
					request.getURI(), body);
			throw new GoogleApiException(GoogleApiFailure.HTTP_STATUS,
					response.getStatusCode().value(), body,
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
	 * Returns the pooled client for an API base URL, creating it on first use.
	 *
	 * @param baseUrl    API base URL
	 * @param clientName pool name for diagnostics
	 * @return pooled REST client
	 */
	RestClient client(String baseUrl, String clientName) {
		return clientsByBaseUrl.computeIfAbsent(baseUrl,
				key -> restClientFactory.createClient(clientName, key, CALL_TIMEOUT));
	}
}
