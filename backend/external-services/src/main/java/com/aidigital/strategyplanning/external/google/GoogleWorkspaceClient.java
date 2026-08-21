package com.aidigital.strategyplanning.external.google;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Narrow outbound boundary for the Google Drive, Slides, and Docs REST APIs.
 *
 * <p>Calls run on the end user's own OAuth token, never on a service account, so every method
 * takes the access token explicitly. Request and response bodies stay as JSON trees: the deck
 * building logic composes Slides requests itself and there is nothing to gain from mapping the
 * whole provider schema.
 */
public interface GoogleWorkspaceClient {

	/**
	 * Copies a Drive file, including files that live in a shared drive.
	 *
	 * @param accessToken user's Google OAuth access token
	 * @param fileId      file to copy
	 * @param name        name for the copy
	 * @return the created file as Drive returns it
	 * @throws GoogleApiException when the call fails or the API answers non-2xx
	 */
	JsonNode copyDriveFile(String accessToken, String fileId, String name);

	/**
	 * Reads a presentation.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to read
	 * @return the presentation as the Slides API returns it
	 * @throws GoogleApiException when the call fails or the API answers non-2xx
	 */
	JsonNode getPresentation(String accessToken, String presentationId);

	/**
	 * Applies a batch of Slides API requests to a presentation.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to update
	 * @param requestsJson   serialized {@code {"requests": [...]}} body
	 * @return the batchUpdate response
	 * @throws GoogleApiException when the call fails or the API answers non-2xx
	 */
	JsonNode batchUpdatePresentation(String accessToken, String presentationId, String requestsJson);

	/**
	 * Applies a batch of Docs API requests to a document.
	 *
	 * @param accessToken  user's Google OAuth access token
	 * @param documentId   document to update
	 * @param requestsJson serialized {@code {"requests": [...]}} body
	 * @return the batchUpdate response
	 * @throws GoogleApiException when the call fails or the API answers non-2xx
	 */
	JsonNode batchUpdateDocument(String accessToken, String documentId, String requestsJson);
}
