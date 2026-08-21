package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * Google connection state for the Standard Category Analysis deck generator.
 * Distinguishes a user who never signed in with Google from one who is signed in
 * but has not granted the Drive/Slides scopes deck creation needs.
 */
public enum GoogleConnectionStatus {

	/**
	 * No Google OAuth grant found — the user did not sign in with Google.
	 */
	NOT_CONNECTED,

	/**
	 * Google is signed in, but the grant is missing the Drive and/or Slides scopes.
	 */
	MISSING_SCOPES,

	/**
	 * Google is signed in and grants both the Drive and Slides scopes.
	 */
	CONNECTED
}
