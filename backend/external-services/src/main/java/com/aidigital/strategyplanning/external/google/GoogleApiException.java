package com.aidigital.strategyplanning.external.google;

import com.aidigital.strategyplanning.external.google.model.GoogleApiFailure;

/**
 * Provider-level failure of a Google API call.
 */
public class GoogleApiException extends RuntimeException {

	private final GoogleApiFailure failure;
	private final int statusCode;
	private final String responseBody;

	/**
	 * Constructs a failure that carries an HTTP status and the provider's response body.
	 *
	 * @param failure      typed failure reason
	 * @param statusCode   HTTP status received, or {@code -1} when no response arrived
	 * @param responseBody body the provider returned, which carries Google's own reason text
	 * @param message      diagnostic description for logs
	 */
	public GoogleApiException(GoogleApiFailure failure, int statusCode, String responseBody,
			String message) {
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
	public GoogleApiException(GoogleApiFailure failure, String message, Throwable cause) {
		super(message, cause);
		this.failure = failure;
		this.statusCode = -1;
		this.responseBody = null;
	}

	/**
	 * Returns the typed reason the call failed.
	 *
	 * @return failure reason
	 */
	public GoogleApiFailure getFailure() {
		return failure;
	}

	/**
	 * Returns the HTTP status received.
	 *
	 * @return HTTP status, or {@code -1} when no response arrived
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Returns the provider's response body.
	 *
	 * <p>Google puts the actual reason for a rejection in the body, not the status line, so this
	 * is what a diagnostic log needs.
	 *
	 * @return response body, or null when the call never produced one
	 */
	public String getResponseBody() {
		return responseBody;
	}
}
