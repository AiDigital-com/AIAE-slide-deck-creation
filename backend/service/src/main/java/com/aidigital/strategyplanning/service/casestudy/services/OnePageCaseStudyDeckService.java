package com.aidigital.strategyplanning.service.casestudy.services;

import java.util.Map;

/**
 * Service contract for generating a One-Page Case Study deck in the user's own Drive.
 */
public interface OnePageCaseStudyDeckService {

	/**
	 * Copies the One-Page template into the user's Drive, fills every template token, and swaps
	 * tagged images: each image whose alt text is {@code {{slot}}} is replaced, in place, by the
	 * image at the mapped URL. Image swaps are best-effort — when Google cannot fetch an image
	 * (e.g. the app is not publicly reachable), the deck keeps the template's images.
	 *
	 * @param userId          Clerk user ID whose Google OAuth token is used
	 * @param deckTitle       file name for the copied deck
	 * @param tokenValues     template token values keyed by token key (without braces)
	 * @param imageUrlsBySlot public image URLs keyed by image slot (alt text without braces)
	 * @return URL of the generated Google Slides deck
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the user is not
	 * connected to Google with Drive and Slides access, or when copying or filling the deck fails
	 */
	String createDeck(String userId, String deckTitle, Map<String, String> tokenValues,
			Map<String, String> imageUrlsBySlot);
}
