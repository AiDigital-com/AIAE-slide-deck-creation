package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Puts the generated category artwork onto the deck's illustrated slides.
 *
 * <p>Each slide is treated differently because the template composes them differently: one slide's
 * placeholder is replaced in place, another's is deleted and re-created so the artwork can sit
 * outside the original frame. Best-effort throughout — a slide keeps its template image when the
 * generated one is unavailable.
 */
public interface DeckImagePlacementService {

	/**
	 * Replaces the template artwork with generated artwork.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation being filled
	 * @param presentation   parsed presentation JSON, as copied
	 * @param category       market category the artwork should depict
	 * @param publicBaseUrl  externally reachable base URL of this backend
	 */
	void replaceSlideImages(String accessToken, String presentationId, JsonNode presentation,
			String category, String publicBaseUrl);
}
