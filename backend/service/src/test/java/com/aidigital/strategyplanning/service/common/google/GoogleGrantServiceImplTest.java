package com.aidigital.strategyplanning.service.common.google;

import com.aidigital.strategyplanning.external.clerk.ClerkOAuthClient;
import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.google.impl.GoogleGrantServiceImpl;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the shared Google grant lookup and its scope check.
 */
class GoogleGrantServiceImplTest {

	private static final String DRIVE = "https://www.googleapis.com/auth/drive";
	private static final String SLIDES = "https://www.googleapis.com/auth/presentations";
	private static final String DOCS = "https://www.googleapis.com/auth/documents";

	@Test
	void shouldAskClerkForTheGoogleProviderTest() {
		// Given: Clerk holds a Google grant for the user
		ClerkOAuthClient clerkOAuthClient = mock(ClerkOAuthClient.class);
		ClerkOAuthGrant expected = new ClerkOAuthGrant("ya29.token", List.of(DRIVE));
		when(clerkOAuthClient.fetchGrant("sk_test", "user_1", "oauth_google")).thenReturn(expected);

		// When: the grant is fetched
		ClerkOAuthGrant actual =
				new GoogleGrantServiceImpl(clerkOAuthClient).fetchGoogleGrant("sk_test", "user_1");

		// Then: the Google provider id is the one Clerk was asked for
		assertThat(actual).isSameAs(expected);
		verify(clerkOAuthClient).fetchGrant("sk_test", "user_1", "oauth_google");
	}

	@Test
	void shouldReturnNoGrantWhenClerkHasNoneTest() {
		// Given: the user has never connected Google
		ClerkOAuthClient clerkOAuthClient = mock(ClerkOAuthClient.class);
		when(clerkOAuthClient.fetchGrant("sk_test", "user_1", "oauth_google")).thenReturn(null);

		// When-Then: the absence passes straight through to the caller
		assertThat(new GoogleGrantServiceImpl(clerkOAuthClient)
				.fetchGoogleGrant("sk_test", "user_1")).isNull();
	}

	@Test
	void shouldAcceptAGrantCarryingEveryRequiredScopeTest() {
		// Given: grants that cover a deck feature and a doc feature
		GoogleGrantServiceImpl grantService =
				new GoogleGrantServiceImpl(mock(ClerkOAuthClient.class));

		// When-Then: extra unrelated scopes do not matter
		assertThat(grantService.hasScopes(List.of(DRIVE, SLIDES, "email"),
				Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES))).isTrue();
		assertThat(grantService.hasScopes(List.of(DRIVE, DOCS, "email"),
				Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.DOCS))).isTrue();
	}

	@Test
	void shouldRejectAGrantMissingDriveOrTheFileTypeScopeTest() {
		// Given: a deck feature that needs both Drive and Slides
		GoogleGrantServiceImpl grantService =
				new GoogleGrantServiceImpl(mock(ClerkOAuthClient.class));
		Set<GoogleOAuthScope> deckScopes = Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES);

		// When-Then: half a grant is not a grant
		assertThat(grantService.hasScopes(List.of(SLIDES, "email"), deckScopes)).isFalse();
		assertThat(grantService.hasScopes(List.of(DRIVE, "email"), deckScopes)).isFalse();
		assertThat(grantService.hasScopes(List.of("email", "profile"), deckScopes)).isFalse();
	}

	@Test
	void shouldNotLetADeckGrantSatisfyADocFeatureTest() {
		// Given: a user who connected Google for the deck builder
		GoogleGrantServiceImpl grantService =
				new GoogleGrantServiceImpl(mock(ClerkOAuthClient.class));

		// When-Then: the doc builder still asks them to reconnect, and vice versa
		assertThat(grantService.hasScopes(List.of(DRIVE, SLIDES),
				Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.DOCS))).isFalse();
		assertThat(grantService.hasScopes(List.of(DRIVE, DOCS),
				Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES))).isFalse();
	}

	@Test
	void shouldPinTheScopeUrisGoogleReportsTest() {
		// Given-When-Then: the enum carries the exact wire values, not paraphrases
		assertThat(GoogleOAuthScope.DRIVE.getUri()).isEqualTo(DRIVE);
		assertThat(GoogleOAuthScope.SLIDES.getUri()).isEqualTo(SLIDES);
		assertThat(GoogleOAuthScope.DOCS.getUri()).isEqualTo(DOCS);
	}

	@Test
	void shouldReturnTheTokenOfAGrantCarryingTheRequiredScopesTest() {
		// Given: a grant covering Drive and Slides
		ClerkOAuthClient clerkOAuthClient = mock(ClerkOAuthClient.class);
		when(clerkOAuthClient.fetchGrant("sk_test", "user_1", "oauth_google"))
				.thenReturn(new ClerkOAuthGrant("ya29.token", List.of(DRIVE, SLIDES)));

		// When: a deck feature requires its token
		String token = new GoogleGrantServiceImpl(clerkOAuthClient)
				.requireAccessToken("sk_test", "user_1", Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES));

		// Then: the raw access token comes back
		assertThat(token).isEqualTo("ya29.token");
	}

	@Test
	void shouldRejectAUserWithoutAGoogleGrantOrTokenTest() {
		// Given: no grant, then a grant with a blank token
		ClerkOAuthClient clerkOAuthClient = mock(ClerkOAuthClient.class);
		when(clerkOAuthClient.fetchGrant("sk_test", "user_1", "oauth_google")).thenReturn(null);
		when(clerkOAuthClient.fetchGrant("sk_test", "user_2", "oauth_google"))
				.thenReturn(new ClerkOAuthGrant(" ", List.of(DRIVE, SLIDES)));
		GoogleGrantServiceImpl grantService = new GoogleGrantServiceImpl(clerkOAuthClient);

		// When-Then: both are told to sign in with Google
		assertThatThrownBy(() -> grantService.requireAccessToken("sk_test", "user_1", Set.of(GoogleOAuthScope.DRIVE)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Not signed in with Google");
		assertThatThrownBy(() -> grantService.requireAccessToken("sk_test", "user_2", Set.of(GoogleOAuthScope.DRIVE)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Not signed in with Google");
	}

	@Test
	void shouldRejectAGrantMissingARequiredScopeTest() {
		// Given: a grant with Drive only
		ClerkOAuthClient clerkOAuthClient = mock(ClerkOAuthClient.class);
		when(clerkOAuthClient.fetchGrant("sk_test", "user_1", "oauth_google"))
				.thenReturn(new ClerkOAuthGrant("ya29.token", List.of(DRIVE)));

		// When-Then: the user is told to reconnect and allow the missing scope
		assertThatThrownBy(() -> new GoogleGrantServiceImpl(clerkOAuthClient)
				.requireAccessToken("sk_test", "user_1", Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("reconnect Google");
	}
}
