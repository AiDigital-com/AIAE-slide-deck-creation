package com.aidigital.strategyplanning.service.categoryanalysis.services;

import java.util.List;
import java.util.Map;

/**
 * Service contract for creating In-Depth Category Analysis decks in the user's Google Slides.
 */
public interface InDepthDeckService {

	/**
	 * Copies the master In-Depth template into the user's Drive, deletes every slide the user
	 * did not check in the "check to include" preview, fills the tokens of the remaining slides,
	 * redraws the slide-8 donut and resizes the slide-18 bars to their values, and returns the
	 * new deck's URL. Chart updates are best-effort and never block deck creation.
	 *
	 * @param userId         Clerk user ID whose Google account owns the new deck
	 * @param deckTitle      file name for the copied presentation
	 * @param selectedSlides slide numbers (1-based, matching the master template's order) to keep
	 *                       in the deck; every other slide is deleted from the copy
	 * @param tokenValues    final reviewed values keyed by template token key, for the checked
	 *                       slides only
	 * @param publicBaseUrl  externally reachable base URL of this backend (scheme + host), used to
	 *                       serve the drawn donut chart image for Google to fetch
	 * @return URL of the newly created Google Slides presentation
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when Google access is
	 * missing, no slides are selected, or a Google API call fails
	 */
	String createInDepthDeck(String userId, String deckTitle, List<Integer> selectedSlides,
	                         Map<String, String> tokenValues, String publicBaseUrl);
}
