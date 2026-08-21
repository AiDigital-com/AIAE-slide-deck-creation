package com.aidigital.strategyplanning.service.categoryanalysis.services;

import java.util.Map;

/**
 * Turns the citations in a finished deck's sources lines into links.
 *
 * <p>Entirely best-effort: if link resolution or the Slides update fails, the deck still ships
 * with plain sources text rather than failing deck creation over a nicety.
 */
public interface DeckSourceHyperlinkService {

	/**
	 * Hyperlinks the citations of a filled presentation.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation being filled
	 * @param tokenValues    final reviewed values keyed by token key
	 */
	void applySourceHyperlinks(String accessToken, String presentationId,
			Map<String, String> tokenValues);
}
