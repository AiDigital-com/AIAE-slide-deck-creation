package com.aidigital.strategyplanning.service.casestudy.services;

import java.util.Map;

/**
 * Service contract for generating a Google Slides case study deck in the user's own Drive.
 */
public interface CaseStudyDeckService {

	/**
	 * Copies the tokenized master template into the user's Drive and fills every template token.
	 *
	 * @param userId      Clerk user ID whose Google OAuth token is used
	 * @param deckTitle   file name for the copied deck
	 * @param tokenValues template token values keyed by token key (without braces)
	 * @return URL of the generated Google Slides deck
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the user is not
	 * connected to Google with Drive and Slides
	 *                                                                          access, or when a Google API call fails
	 */
	String createDeck(String userId, String deckTitle, Map<String, String> tokenValues);
}
