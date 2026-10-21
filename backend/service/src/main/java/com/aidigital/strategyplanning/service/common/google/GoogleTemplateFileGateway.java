package com.aidigital.strategyplanning.service.common.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Copy-then-fill view of Google Drive for features built on a tokenized master template.
 *
 * <p>Wraps the outbound client so every provider failure arrives as an application error naming
 * the action the user was attempting — "copy the case study template into your Google Drive"
 * rather than a bare HTTP status. The case study deck and the RFP outline doc follow the same
 * two-step flow and differ only in their template id, their file type, and that action text.
 */
public interface GoogleTemplateFileGateway {

	/**
	 * Copies a tokenized master template into the user's Drive.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param templateFileId Drive id of the master template
	 * @param title          file name for the copy
	 * @param action         human-readable action used in error messages
	 * @return Drive id of the copy
	 */
	String copyTemplate(String accessToken, String templateFileId, String title, String action);

	/**
	 * Applies replaceAllText requests to a copied presentation.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to fill
	 * @param requests       Slides API requests; an empty array is a no-op
	 * @param action         human-readable action used in error messages
	 */
	void fillPresentation(String accessToken, String presentationId, ArrayNode requests, String action);

	/**
	 * Reads a copied presentation, for features that need to locate tagged elements before
	 * updating them.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to read
	 * @param action         human-readable action used in error messages
	 * @return the presentation as the Slides API returns it
	 */
	JsonNode readPresentation(String accessToken, String presentationId, String action);

	/**
	 * Applies replaceAllText requests to a copied document.
	 *
	 * @param accessToken user's Google OAuth access token
	 * @param documentId  document to fill
	 * @param requests    Docs API requests; an empty array is a no-op
	 * @param action      human-readable action used in error messages
	 */
	void fillDocument(String accessToken, String documentId, ArrayNode requests, String action);

	/**
	 * Exports a Slides master as raw PPTX bytes, for features that assemble a page from its OOXML
	 * instead of copying it. The master is only read, never changed.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to export
	 * @param action         human-readable action used in error messages
	 * @return the PPTX bytes
	 */
	byte[] exportPresentation(String accessToken, String presentationId, String action);

	/**
	 * Uploads PPTX bytes to the user's Drive as a brand-new native Slides presentation.
	 *
	 * @param accessToken user's Google OAuth access token
	 * @param title       file name for the new presentation
	 * @param pptxBytes   PPTX bytes to upload
	 * @param action      human-readable action used in error messages
	 * @return Drive id of the new presentation
	 */
	String uploadPresentation(String accessToken, String title, byte[] pptxBytes, String action);
}
