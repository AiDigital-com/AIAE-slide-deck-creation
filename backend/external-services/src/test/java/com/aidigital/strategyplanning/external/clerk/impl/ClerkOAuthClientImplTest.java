package com.aidigital.strategyplanning.external.clerk.impl;

import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link ClerkOAuthClientImpl}.
 *
 * <p>Every unusable answer must resolve to null rather than an exception: to the caller a missing
 * grant, a revoked grant, and an unreachable Clerk all mean "the user must reconnect Google".
 */
class ClerkOAuthClientImplTest {

	private ClerkOAuthClientImpl client() {
		return new ClerkOAuthClientImpl(mock(PooledRestClientFactory.class), new ObjectMapper());
	}

	@Test
	void shouldNotCallClerkWithoutASecretKeyTest() {
		// Given: a feature with no Clerk secret configured
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		ClerkOAuthClientImpl client = new ClerkOAuthClientImpl(factory, new ObjectMapper());

		// When-Then: the lookup short-circuits without building a client
		assertThat(client.fetchGrant("", "user_1", "oauth_google")).isNull();
		assertThat(client.fetchGrant(null, "user_1", "oauth_google")).isNull();
		verifyNoInteractions(factory);
	}

	@Test
	void shouldReadTheGrantFromABareArrayResponseTest() {
		// Given: the array shape Clerk returns
		String body = "[{\"token\":\"ya29.token\",\"scopes\":"
				+ "[\"https://www.googleapis.com/auth/drive\"]}]";

		// When: the response is parsed
		ClerkOAuthGrant grant = client().parseGrant(body);

		// Then: the token and its scopes come through
		assertThat(grant).isNotNull();
		assertThat(grant.token()).isEqualTo("ya29.token");
		assertThat(grant.scopes()).containsExactly("https://www.googleapis.com/auth/drive");
	}

	@Test
	void shouldReadTheGrantFromADataWrappedResponseTest() {
		// Given: the data-wrapped shape Clerk also returns
		String body = "{\"data\":[{\"token\":\"ya29.token\",\"scopes\":[]}]}";

		// When-Then: both shapes are accepted so a Clerk API change does not break sign-in
		ClerkOAuthGrant grant = client().parseGrant(body);
		assertThat(grant).isNotNull();
		assertThat(grant.token()).isEqualTo("ya29.token");
		assertThat(grant.scopes()).isEmpty();
	}

	@Test
	void shouldTreatAnyUnusableResponseAsNoGrantTest() {
		// Given: responses that carry no usable grant
		// When-Then: each resolves to null instead of throwing
		for (String body : new String[]{"[]", "{}", "{\"data\":[]}",
				"{\"data\":[{\"token\":\"\"}]}", "not json at all"}) {
			assertThat(client().parseGrant(body)).isNull();
		}
	}

	@Test
	void shouldReadTheScopeArrayFromAClerkEntryTest() throws Exception {
		// Given: a Clerk oauth-access-token entry
		com.fasterxml.jackson.databind.JsonNode entry = new ObjectMapper().readTree(
				"{\"token\":\"t\",\"scopes\":[\"email\",\"https://www.googleapis.com/auth/drive\"]}");

		// When-Then: the scopes come through in order
		assertThat(client().scopesOf(entry))
				.containsExactly("email", "https://www.googleapis.com/auth/drive");
	}
}
