package com.aidigital.strategyplanning.service.common.google;

import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;

import java.util.List;
import java.util.Set;

/**
 * Resolves the Google grant a deck- or doc-building feature needs from the caller's Clerk session.
 *
 * <p>Shared by every feature that writes into the user's own Drive: each passes its own Clerk
 * secret key and names the scopes its own output format requires.
 */
public interface GoogleGrantService {

	/**
	 * Fetches the caller's Google grant.
	 *
	 * @param clerkSecretKey Clerk secret key of the calling feature
	 * @param userId         Clerk user id
	 * @return the grant, or null when the user has not connected Google
	 */
	ClerkOAuthGrant fetchGoogleGrant(String clerkSecretKey, String userId);

	/**
	 * Reports whether a grant carries every scope a feature requires.
	 *
	 * @param grantedScopes  scopes granted on the user's Google token
	 * @param requiredScopes scopes the calling feature needs
	 * @return true when every required scope is present
	 */
	boolean hasScopes(List<String> grantedScopes, Set<GoogleOAuthScope> requiredScopes);
}
