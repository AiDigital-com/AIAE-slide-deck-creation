package com.aidigital.strategyplanning.external.openai;

import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;

/**
 * Provider-level failure of an OpenAI call.
 *
 * <p>Carries a typed {@link OpenAiFailure} plus the HTTP status when one was received, so each
 * calling feature can translate the failure into its own application error and message rather
 * than inheriting wording from the transport layer.
 */
public class OpenAiExternalException extends RuntimeException {

	private final OpenAiFailure failure;
	private final int statusCode;
	private final String responseBody;

	/**
	 * Constructs a failure that carries an HTTP status.
	 *
	 * @param failure    typed failure reason
	 * @param statusCode HTTP status received, or {@code -1} when no response arrived
	 * @param message    diagnostic description for logs
	 */
	public OpenAiExternalException(OpenAiFailure failure, int statusCode, String message) {
		this(failure, statusCode, message, "");
	}

	/**
	 * Constructs a failure that carries an HTTP status and the provider's response body.
	 *
	 * @param failure      typed failure reason
	 * @param statusCode   HTTP status received, or {@code -1} when no response arrived
	 * @param message      diagnostic description for logs
	 * @param responseBody raw response body, for callers that log the provider's own error text
	 */
	public OpenAiExternalException(OpenAiFailure failure, int statusCode, String message,
			String responseBody) {
		super(message);
		this.failure = failure;
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	/**
	 * Constructs a failure wrapping a transport-level cause.
	 *
	 * @param failure typed failure reason
	 * @param message diagnostic description for logs
	 * @param cause   underlying exception
	 */
	public OpenAiExternalException(OpenAiFailure failure, String message, Throwable cause) {
		super(message, cause);
		this.failure = failure;
		this.statusCode = -1;
		this.responseBody = "";
	}

	/**
	 * Returns the typed reason the call failed.
	 *
	 * @return failure reason
	 */
	public OpenAiFailure getFailure() {
		return failure;
	}

	/**
	 * Returns the HTTP status received from the provider.
	 *
	 * @return HTTP status, or {@code -1} when no response arrived
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Returns the provider's raw response body.
	 *
	 * @return response body, empty when no response was received
	 */
	public String getResponseBody() {
		return responseBody;
	}
}
