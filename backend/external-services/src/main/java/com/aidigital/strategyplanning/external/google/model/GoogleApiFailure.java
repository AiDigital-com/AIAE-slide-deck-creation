package com.aidigital.strategyplanning.external.google.model;

/**
 * Why a Google API call did not produce a usable response.
 *
 * <p>Callers phrase the user-facing message themselves, naming the action that was being attempted,
 * so the transport layer only reports the shape of the failure.
 */
public enum GoogleApiFailure {

	/** The API answered with a non-2xx status. */
	HTTP_STATUS,

	/** The call never completed, or the body could not be read. */
	TRANSPORT,

	/** The calling thread was interrupted while waiting. */
	INTERRUPTED
}
