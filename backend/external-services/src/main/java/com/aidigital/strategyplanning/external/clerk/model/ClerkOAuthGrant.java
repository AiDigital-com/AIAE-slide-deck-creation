package com.aidigital.strategyplanning.external.clerk.model;

import java.util.List;

/**
 * A provider OAuth grant Clerk holds on a user's behalf.
 *
 * @param token  provider access token
 * @param scopes scopes granted on the token, empty when Clerk reports none
 */
public record ClerkOAuthGrant(String token, List<String> scopes) {

}
