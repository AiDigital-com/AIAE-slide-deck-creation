package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageCaseStudyDeckService;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDeckRequestAssembler;
import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.google.GoogleTemplateFileGateway;
import com.aidigital.strategyplanning.service.common.google.TokenReplacementRequestFactory;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Google Drive/Slides implementation of {@link OnePageCaseStudyDeckService}. Resolves the user's
 * Google access token through Clerk, copies the One-Page template into the user's Drive, fills
 * every {@code {{token}}} in one batchUpdate, then reads the copy back to find the tagged icons
 * and swaps them in a second, best-effort batchUpdate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOnePageCaseStudyDeckServiceImpl implements OnePageCaseStudyDeckService {

	private static final Set<GoogleOAuthScope> REQUIRED_SCOPES =
			Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES);
	private static final String COPY_ACTION = "copy the one-page case study template into your Google Drive";
	private static final String FILL_ACTION = "fill the one-page case study deck";
	private static final String READ_ACTION = "read the one-page case study deck";
	private static final String SWAP_ICONS_ACTION = "swap the one-page case study icons";
	private static final String DECK_URL_PREFIX = "https://docs.google.com/presentation/d/";
	private static final String DECK_URL_SUFFIX = "/edit";

	private final CaseStudyProperties properties;
	private final GoogleGrantService googleGrantService;
	private final GoogleTemplateFileGateway templateFileGateway;
	private final TokenReplacementRequestFactory requestFactory;
	private final OnePageDeckRequestAssembler requestAssembler;

	@Override
	public String createDeck(String userId, String deckTitle, Map<String, String> tokenValues,
			Map<String, String> imageUrlsBySlot) {
		String accessToken = googleGrantService.requireAccessToken(
				properties.getClerkSecretKey(), userId, REQUIRED_SCOPES);
		String presentationId = templateFileGateway.copyTemplate(accessToken,
				properties.getOnePageTemplatePresentationId(), deckTitle, COPY_ACTION);
		templateFileGateway.fillPresentation(accessToken, presentationId,
				requestFactory.buildReplaceTextRequests(tokenValues), FILL_ACTION);
		if (!imageUrlsBySlot.isEmpty()) {
			swapIcons(accessToken, presentationId, imageUrlsBySlot);
		}
		return DECK_URL_PREFIX + presentationId + DECK_URL_SUFFIX;
	}

	/**
	 * Replaces the tagged template icons with the chosen library icons. Best-effort: when Google
	 * cannot read the deck or fetch an icon image, the deck keeps the template's icons and still
	 * has all its text.
	 *
	 * @param accessToken     user's Google OAuth access token
	 * @param presentationId  freshly filled presentation
	 * @param imageUrlsBySlot public image URLs keyed by slot
	 */
	void swapIcons(String accessToken, String presentationId, Map<String, String> imageUrlsBySlot) {
		try {
			JsonNode presentation = templateFileGateway.readPresentation(accessToken, presentationId, READ_ACTION);
			templateFileGateway.fillPresentation(accessToken, presentationId,
					requestAssembler.buildReplaceImageRequests(presentation, imageUrlsBySlot), SWAP_ICONS_ACTION);
		} catch (RuntimeException e) {
			log.warn("Kept the template's icons in deck {}: {}", presentationId, e.getMessage());
		}
	}
}
