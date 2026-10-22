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

	/**
	 * The provider answered 200 but reported that the output was cut off before it finished: a
	 * chat completion whose {@code finish_reason} is {@code length}, or a response whose
	 * {@code status} is {@code incomplete}. The content is unusable because a truncated answer is
	 * almost never valid JSON, and the caller should ask for less at once rather than retry.
	 */
	INCOMPLETE,

	/** The call never completed: connection failure, timeout, or unreadable body. */
	TRANSPORT,

	/** The calling thread was interrupted while waiting for the provider. */
	INTERRUPTED
}
