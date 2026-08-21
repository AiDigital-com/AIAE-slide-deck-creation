package com.aidigital.strategyplanning.external.clerk.impl;

import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Transport-level tests for {@link ClerkOAuthClientImpl} against a stub HTTP server.
 *
 * <p>The Clerk secret key is the credential here, not the end user's token, and it must never
 * appear anywhere but the Authorization header. Everything unusable has to become null, because
 * to the caller a missing grant and an unreachable Clerk both mean "reconnect Google".
 */
class ClerkOAuthClientTransportTest {

	private static final String SECRET = "sk_test_secret";

	/**
	 * Builds a client whose pooled REST client points at the stub server.
	 *
	 * @param server stub HTTP server
	 * @return client under test
	 */
	private ClerkOAuthClientImpl clientFor(MockWebServer server) {
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		when(factory.createClient("clerk", "https://api.clerk.com/v1", Duration.ofSeconds(20)))
				.thenReturn(RestClient.builder().baseUrl(server.url("/").toString()).build());
		return new ClerkOAuthClientImpl(factory, new ObjectMapper());
	}

	@Test
	void shouldFetchTheGrantForAUserAndProviderTest() throws Exception {
		// Given: Clerk holds a Google grant for the user
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody(
					"[{\"token\":\"ya29.token\",\"scopes\":"
							+ "[\"https://www.googleapis.com/auth/drive\"]}]"));

			// When: the grant is fetched
			ClerkOAuthGrant grant = clientFor(server).fetchGrant(SECRET, "user_1", "oauth_google");

			// Then: the user id and provider are in the path and the secret is only a header
			assertThat(grant).isNotNull();
			assertThat(grant.token()).isEqualTo("ya29.token");
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("GET");
			assertThat(request.getPath())
					.isEqualTo("/users/user_1/oauth_access_tokens/oauth_google");
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + SECRET);
		}
	}

	@Test
	void shouldEncodeAUserIdSafelyIntoThePathTest() throws Exception {
		// Given: a user id that would break a hand-concatenated URL
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

			// When: the grant is fetched for it
			clientFor(server).fetchGrant(SECRET, "user/../admin", "oauth_google");

			// Then: the segment is encoded rather than traversing to another Clerk resource
			assertThat(server.takeRequest().getPath())
					.doesNotContain("/admin/")
					.contains("oauth_access_tokens/oauth_google");
		}
	}

	@Test
	void shouldTreatANonSuccessStatusAsNoGrantTest() throws IOException {
		// Given: Clerk rejects the secret key
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(401).setBody("{}"));

			// When-Then: the user is asked to reconnect rather than shown a 401
			assertThat(clientFor(server).fetchGrant(SECRET, "user_1", "oauth_google")).isNull();
		}
	}

	@Test
	void shouldTreatAnUnreachableClerkAsNoGrantTest() throws IOException {
		// Given: Clerk is unreachable
		MockWebServer server = new MockWebServer();
		server.start();
		ClerkOAuthClientImpl client = clientFor(server);
		server.close();

		// When-Then: the transport failure is swallowed into "no grant", never an exception
		assertThat(client.fetchGrant(SECRET, "user_1", "oauth_google")).isNull();
	}
}
