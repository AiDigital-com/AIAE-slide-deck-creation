package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;

import java.util.Map;

/**
 * Service contract for creating Standard Category Analysis decks in the user's Google Slides.
 */
public interface GoogleDeckService {

	/**
	 * Reports the user's Google connection state, distinguishing "not signed in with Google"
	 * from "signed in but missing the Drive/Slides scopes" via Clerk SSO.
	 *
	 * @param userId Clerk user ID
	 * @return the detailed Google connection status
	 */
	GoogleConnectionStatus checkGoogleConnection(String userId);

	/**
	 * Copies the master template into the user's Drive, fills every template token, replaces the
	 * image placeholders on slides 1-4 with friendly, on-vertical generated photographs, and
	 * returns the new deck's URL. Image replacement is best-effort and never blocks deck creation.
	 *
	 * @param userId        Clerk user ID whose Google account owns the new deck
	 * @param deckTitle     file name for the copied presentation
	 * @param category      market category used to theme the generated slide images
	 * @param tokenValues   final reviewed values keyed by template token key
	 * @param publicBaseUrl externally reachable base URL of this backend (scheme + host), used to
	 *                      host generated images so Google Slides can fetch them
	 * @return URL of the newly created Google Slides presentation
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when Google access is
	 * missing or a Google API call fails
	 */
	String createDeck(String userId, String deckTitle, String category,
	                  Map<String, String> tokenValues, String publicBaseUrl);
}
