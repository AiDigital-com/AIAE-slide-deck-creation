package com.aidigital.strategyplanning.service.categoryanalysis.models;

import java.util.List;

/**
 * A user's Google OAuth grant obtained through Clerk SSO.
 *
 * @param token  Google OAuth access token
 * @param scopes OAuth scopes granted on the token (may be empty when Clerk reports none)
 */
public record GoogleOAuthGrant(
		String token,
		List<String> scopes
) {

}
