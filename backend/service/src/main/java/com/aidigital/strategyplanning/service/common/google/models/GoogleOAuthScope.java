package com.aidigital.strategyplanning.service.common.google.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Google OAuth scopes this product asks the user to grant.
 *
 * <p>Each feature that writes into the user's own Drive needs Drive plus the scope for the file
 * type it produces: Slides for a deck, Docs for a document. The URI is the wire value Google
 * reports back on the granted token.
 */
@Getter
@RequiredArgsConstructor
public enum GoogleOAuthScope {

	/** Read/write access to the user's Drive, needed to copy a master template. */
	DRIVE("https://www.googleapis.com/auth/drive"),

	/** Read/write access to Google Slides presentations. */
	SLIDES("https://www.googleapis.com/auth/presentations"),

	/** Read/write access to Google Docs documents. */
	DOCS("https://www.googleapis.com/auth/documents");

	private final String uri;
}
