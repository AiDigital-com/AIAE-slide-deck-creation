package com.aidigital.strategyplanning.external.clerk;

import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;

/**
 * Reads the provider OAuth grants Clerk stores for a user.
 *
 * <p>The application never holds Google credentials itself: each user's Drive and Slides work runs
 * on their own grant, fetched through Clerk at the moment it is needed.
 */
public interface ClerkOAuthClient {

	/**
	 * Fetches a user's grant for one OAuth provider.
	 *
	 * @param secretKey Clerk secret key of the calling feature
	 * @param userId    Clerk user id
	 * @param provider  Clerk provider id, for example {@code oauth_google}
	 * @return the grant, or null when the user has none, the key is absent, or Clerk is
	 *         unreachable — callers treat all three as "not connected"
	 */
	ClerkOAuthGrant fetchGrant(String secretKey, String userId, String provider);
}
