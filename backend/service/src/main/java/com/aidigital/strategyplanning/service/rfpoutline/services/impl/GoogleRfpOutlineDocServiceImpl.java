package com.aidigital.strategyplanning.service.rfpoutline.services.impl;

import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.google.GoogleTemplateFileGateway;
import com.aidigital.strategyplanning.service.common.google.TokenReplacementRequestFactory;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import com.aidigital.strategyplanning.service.rfpoutline.config.RfpOutlineProperties;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * Google Drive/Docs implementation of {@link RfpOutlineDocService}. Mirrors the Case Study deck flow:
 * fetches the user's Google OAuth access token from Clerk, copies the tokenized master template
 * into the user's Drive, and fills every {@code {{token}}} with the reviewed RFP outline values via a
 * single batchUpdate call against the Docs API.
 */
@Service
@RequiredArgsConstructor
public class GoogleRfpOutlineDocServiceImpl implements RfpOutlineDocService {

	private static final Set<GoogleOAuthScope> REQUIRED_SCOPES =
			Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.DOCS);
	private static final String COPY_ACTION = "copy the RFP outline template into your Google Drive";
	private static final String FILL_ACTION = "fill the RFP outline doc";
	private static final String DOC_URL_PREFIX = "https://docs.google.com/document/d/";
	private static final String DOC_URL_SUFFIX = "/edit";

	private final RfpOutlineProperties properties;
	private final GoogleGrantService googleGrantService;
	private final GoogleTemplateFileGateway templateFileGateway;
	private final TokenReplacementRequestFactory requestFactory;

	@Override
	public String createDoc(String userId, String docTitle, Map<String, String> tokenValues) {
		String accessToken = grantedAccessToken(userId);
		String documentId = templateFileGateway.copyTemplate(accessToken,
				properties.getTemplateDocId(), docTitle, COPY_ACTION);
		templateFileGateway.fillDocument(accessToken, documentId,
				requestFactory.buildReplaceTextRequests(tokenValues), FILL_ACTION);
		return DOC_URL_PREFIX + documentId + DOC_URL_SUFFIX;
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
					"Not signed in with Google — sign in with your Google account to create the doc");
		}
		if (!googleGrantService.hasScopes(grant.scopes(), REQUIRED_SCOPES)) {
			throw new AppException(ErrorReason.C003,
					"Google Drive & Docs access not granted — reconnect Google and allow Drive & Docs access");
		}
		return grant.token();
	}
}
