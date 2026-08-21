package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleDeckService;
import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.service.categoryanalysis.services.DeckImagePlacementService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.DeckSourceHyperlinkService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleSlidesGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesPresentationInspector;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesRequestFactory;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardDeckTemplateAdjuster;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Google Drive/Slides implementation of {@link GoogleDeckService}.
 * Uses the user's Google OAuth access token (obtained through Clerk SSO) to copy the
 * master template into the user's own Drive, fill all template tokens, and replace the
 * image placeholders on slides 1-4 with friendly, on-vertical generated photographs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDeckServiceImpl implements GoogleDeckService {

	private static final String SOURCES_KEY_SUFFIX = "_sources";
	private static final Set<GoogleOAuthScope> REQUIRED_SCOPES =
			Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES);

	private final CategoryAnalysisProperties properties;
	private final SlidesPresentationInspector inspector;
	private final SlidesRequestFactory requestFactory;
	private final StandardDeckTemplateAdjuster templateAdjuster;
	private final GoogleGrantService googleGrantService;
	private final GoogleSlidesGateway slidesGateway;
	private final DeckSourceHyperlinkService hyperlinkService;
	private final DeckImagePlacementService imagePlacementService;

	@Override
	public GoogleConnectionStatus checkGoogleConnection(String userId) {
		ClerkOAuthGrant grant = googleGrantService.fetchGoogleGrant(
				properties.getClerkSecretKey(), userId);
		if (grant == null || !StringUtils.hasText(grant.token())) {
			return GoogleConnectionStatus.NOT_CONNECTED;
		}
		return googleGrantService.hasScopes(grant.scopes(), REQUIRED_SCOPES)
				? GoogleConnectionStatus.CONNECTED
				: GoogleConnectionStatus.MISSING_SCOPES;
	}

	@Override
	public String createDeck(String userId, String deckTitle, String category,
	                         Map<String, String> tokenValues, String publicBaseUrl) {
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
		String accessToken = grant.token();
		String presentationId = slidesGateway.copyTemplate(accessToken, deckTitle);
		JsonNode presentation = slidesGateway.getPresentation(accessToken, presentationId);
		List<String> slideIds = inspector.extractSlideIds(presentation);
		try {
			applyTemplateAdjustments(accessToken, presentationId, presentation);
		} catch (RuntimeException e) {
			log.warn("Skipping template adjustments for deck {}: {}", presentationId, e.getMessage());
		}
		slidesGateway.batchUpdate(accessToken, presentationId,
				requestFactory.buildReplaceTextRequests(tokenValues, slideIds));
		try {
			hyperlinkService.applySourceHyperlinks(accessToken, presentationId, tokenValues);
		} catch (RuntimeException e) {
			log.warn("Skipping source hyperlinks for deck {}: {}", presentationId, e.getMessage());
		}
		try {
			imagePlacementService.replaceSlideImages(accessToken, presentationId, presentation,
					category, publicBaseUrl);
		} catch (RuntimeException e) {
			log.warn("Skipping generated slide images for deck {}: {}", presentationId, e.getMessage());
		}
		return "https://docs.google.com/presentation/d/" + presentationId + "/edit";
	}

	/**
	 * Applies one-time layout adjustments to a freshly copied deck so it matches the approved
	 * reference layout: on slide 3 the large decorative image is removed and the circle-masked
	 * image is enlarged to fill that space and the section label buried behind it is deleted;
	 * on slide 2 the chart title box is deleted, the trend bullets box is bolded end-to-end at
	 * 11pt, and the "WHAT'S TRENDING?" heading is recolored to the brand green at 18pt; on the
	 * title slide the
	 * "CATEGORY ANALYSIS" heading is recolored to the deck's brand green.
	 * All adjustments are best-effort — when an element cannot be found the adjustment is
	 * skipped so deck creation never fails.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to adjust
	 * @param presentation   parsed presentation JSON (as copied, before adjustments)
	 */
	public void applyTemplateAdjustments(String accessToken, String presentationId, JsonNode presentation) {
		ArrayNode requests = templateAdjuster.buildAdjustmentRequests(presentation);
		if (!requests.isEmpty()) {
			slidesGateway.batchUpdate(accessToken, presentationId, requests);
		}
	}

}
