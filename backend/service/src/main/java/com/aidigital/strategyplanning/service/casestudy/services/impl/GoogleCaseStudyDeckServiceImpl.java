package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDeckService;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.google.GoogleTemplateFileGateway;
import com.aidigital.strategyplanning.service.common.google.TokenReplacementRequestFactory;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * Google Drive/Slides implementation of {@link CaseStudyDeckService}. Mirrors the Category
 * Analysis deck flow: fetches the user's Google OAuth access token from Clerk, copies the
 * tokenized master template into the user's Drive, and fills every {@code {{token}}} with the
 * reviewed case study values via a single batchUpdate call.
 */
@Service
@RequiredArgsConstructor
public class GoogleCaseStudyDeckServiceImpl implements CaseStudyDeckService {

	private static final Set<GoogleOAuthScope> REQUIRED_SCOPES =
			Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES);
	private static final String COPY_ACTION = "copy the case study template into your Google Drive";
	private static final String FILL_ACTION = "fill the case study deck";
	private static final String DECK_URL_PREFIX = "https://docs.google.com/presentation/d/";
	private static final String DECK_URL_SUFFIX = "/edit";

	private final CaseStudyProperties properties;
	private final GoogleGrantService googleGrantService;
	private final GoogleTemplateFileGateway templateFileGateway;
	private final TokenReplacementRequestFactory requestFactory;

	@Override
	public String createDeck(String userId, String deckTitle, Map<String, String> tokenValues) {
		String accessToken = grantedAccessToken(userId);
		String presentationId = templateFileGateway.copyTemplate(accessToken,
				properties.getTemplatePresentationId(), deckTitle, COPY_ACTION);
		templateFileGateway.fillPresentation(accessToken, presentationId,
				requestFactory.buildReplaceTextRequests(tokenValues), FILL_ACTION);
		return DECK_URL_PREFIX + presentationId + DECK_URL_SUFFIX;
	}

	/**
	 * Resolves the caller's Google access token, rejecting a missing connection or missing scopes
	 * with the message that tells the user what to do about it.
	 *
	 * @param userId Clerk user ID
	 * @return the user's Google OAuth access token
	 * @throws AppException with C003 reason when Google is not connected or scopes are missing
	 */
	String grantedAccessToken(String userId) {
		ClerkOAuthGrant grant = googleGrantService.fetchGoogleGrant(
				properties.getClerkSecretKey(), userId);
		if (grant == null || !StringUtils.hasText(grant.token())) {
			throw new AppException(ErrorReason.C003,
					"Not signed in with Google — sign in with your Google account to create the deck");
		}
		if (!googleGrantService.hasScopes(grant.scopes(), REQUIRED_SCOPES)) {
			throw new AppException(ErrorReason.C003,
					"Google Drive & Slides access not granted — reconnect Google and allow Drive & Slides access");
		}
		return grant.token();
	}
}
