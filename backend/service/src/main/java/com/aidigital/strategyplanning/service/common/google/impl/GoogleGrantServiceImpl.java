package com.aidigital.strategyplanning.service.common.google.impl;

import com.aidigital.strategyplanning.external.clerk.ClerkOAuthClient;
import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Default implementation of {@link GoogleGrantService}.
 */
@Service
@RequiredArgsConstructor
public class GoogleGrantServiceImpl implements GoogleGrantService {

	private static final String GOOGLE_PROVIDER = "oauth_google";

	private final ClerkOAuthClient clerkOAuthClient;

	@Override
	public ClerkOAuthGrant fetchGoogleGrant(String clerkSecretKey, String userId) {
		return clerkOAuthClient.fetchGrant(clerkSecretKey, userId, GOOGLE_PROVIDER);
	}

	@Override
	public boolean hasScopes(List<String> grantedScopes, Set<GoogleOAuthScope> requiredScopes) {
		for (GoogleOAuthScope required : requiredScopes) {
			if (!grantedScopes.contains(required.getUri())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String requireAccessToken(String clerkSecretKey, String userId, Set<GoogleOAuthScope> requiredScopes) {
		ClerkOAuthGrant grant = fetchGoogleGrant(clerkSecretKey, userId);
		if (grant == null || !StringUtils.hasText(grant.token())) {
			throw new AppException(ErrorReason.C003,
					"Not signed in with Google — sign in with your Google account to create the deck");
		}
		if (!hasScopes(grant.scopes(), requiredScopes)) {
			throw new AppException(ErrorReason.C003,
					"Google Drive & Slides access not granted — reconnect Google and allow Drive & Slides access");
		}
		return grant.token();
	}
}
