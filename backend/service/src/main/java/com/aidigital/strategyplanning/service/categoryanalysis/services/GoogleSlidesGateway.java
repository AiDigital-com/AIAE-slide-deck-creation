package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * The deck flow's view of Google Drive and Slides.
 *
 * <p>Wraps the outbound client so every provider failure arrives as an application error that
 * names the action the user was attempting — "copy the master template into your Google Drive"
 * rather than an HTTP status on its own.
 */
public interface GoogleSlidesGateway {

	/**
	 * Copies the master template into the user's Drive.
	 *
	 * @param accessToken user's Google OAuth access token
	 * @param deckTitle   file name for the copy
	 * @return presentation id of the copy
	 */
	String copyTemplate(String accessToken, String deckTitle);

	/**
	 * Reads a presentation.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to read
	 * @return the presentation as the Slides API returns it
	 */
	JsonNode getPresentation(String accessToken, String presentationId);

	/**
	 * Applies a batch of Slides requests.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to update
	 * @param requests       Slides API requests
	 */
	void batchUpdate(String accessToken, String presentationId, ArrayNode requests);
}
