package com.aidigital.strategyplanning.external.openai.model;

/**
 * Why an OpenAI call did not produce usable content.
 *
 * <p>Callers map these onto their own feature-specific application errors, so the transport layer
 * never decides what the user is told.
 */
public enum OpenAiFailure {

	/** The provider answered with a non-200 status. */
	HTTP_STATUS,

	/** The provider answered 200 but the message content was absent or blank. */
	EMPTY_CONTENT,

	/** The call never completed: connection failure, timeout, or unreadable body. */
	TRANSPORT,

	/** The calling thread was interrupted while waiting for the provider. */
	INTERRUPTED
}
