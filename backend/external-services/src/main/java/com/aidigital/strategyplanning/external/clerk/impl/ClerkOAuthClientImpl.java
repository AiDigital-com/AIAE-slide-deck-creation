package com.aidigital.strategyplanning.external.clerk.impl;

import com.aidigital.strategyplanning.external.clerk.ClerkOAuthClient;
import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pooled implementation of {@link ClerkOAuthClient}.
 *
 * <p>Every failure resolves to null rather than an exception: a missing grant, a revoked grant,
 * and an unreachable Clerk are the same thing to the caller — the user must reconnect — and the
 * UI already guides them through that.
 *
 * <p>Every {@code exchange} call passes {@code close = true}. Spring closes the response only in
 * that case, and each handler here reads the whole body into memory before returning, so there is
 * nothing left to stream. With {@code false} the pooled connection is never released and the pool
 * starves after {@code max-connections-per-route} calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClerkOAuthClientImpl implements ClerkOAuthClient {

	private static final String CLERK_API_BASE = "https://api.clerk.com/v1";
	private static final String CLIENT_NAME = "clerk";
	private static final Duration GRANT_TIMEOUT = Duration.ofSeconds(20);

	private final PooledRestClientFactory restClientFactory;
	private final ObjectMapper objectMapper;
	private final AtomicReference<RestClient> client = new AtomicReference<>();

	@Override
	public ClerkOAuthGrant fetchGrant(String secretKey, String userId, String provider) {
		if (secretKey == null || secretKey.isBlank()) {
			return null;
		}
		String body = fetchBody(secretKey, userId, provider);
		return body == null ? null : parseGrant(body);
	}

	/**
	 * Performs the Clerk call and returns the raw body, or null when it did not succeed.
	 *
	 * @param secretKey Clerk secret key
	 * @param userId    Clerk user id
	 * @param provider  Clerk provider id
	 * @return raw response body, or null on any non-success
	 */
	String fetchBody(String secretKey, String userId, String provider) {
		try {
			return client()
					.get()
					.uri("/users/{userId}/oauth_access_tokens/{provider}", userId, provider)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
					.exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
							? new String(response.getBody().readAllBytes())
							: null, true);
		} catch (RuntimeException e) {
			log.warn("Clerk OAuth grant lookup failed: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Reads the first grant out of a Clerk response, tolerating both the bare-array and the
	 * {@code data}-wrapped shapes Clerk has used.
	 *
	 * @param body raw response body
	 * @return the grant, or null when the response carries none
	 */
	ClerkOAuthGrant parseGrant(String body) {
		JsonNode root;
		try {
			root = objectMapper.readTree(body);
		} catch (IOException e) {
			log.warn("Clerk OAuth grant response was not valid JSON");
			return null;
		}
		JsonNode entries = root.isArray() ? root : root.path("data");
		if (!entries.isArray() || entries.isEmpty()) {
			return null;
		}
		JsonNode entry = entries.path(0);
		String token = entry.path("token").asText("");
		if (token.isBlank()) {
			return null;
		}
		return new ClerkOAuthGrant(token, scopesOf(entry));
	}

	/**
	 * Extracts the granted scopes from a Clerk oauth-access-token entry.
	 *
	 * @param entry a single Clerk oauth-access-token entry
	 * @return granted scopes, never null
	 */
	List<String> scopesOf(JsonNode entry) {
		List<String> scopes = new ArrayList<>();
		entry.path("scopes").forEach(scope -> scopes.add(scope.asText()));
		return scopes;
	}

	/**
	 * Returns the pooled Clerk client, creating it on first use.
	 *
	 * @return pooled REST client
	 */
	RestClient client() {
		RestClient existing = client.get();
		if (existing != null) {
			return existing;
		}
		RestClient created = restClientFactory.createClient(CLIENT_NAME, CLERK_API_BASE, GRANT_TIMEOUT);
		return client.compareAndSet(null, created) ? created : client.get();
	}
}
