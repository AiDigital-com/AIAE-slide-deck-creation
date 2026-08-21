package com.aidigital.strategyplanning.external.common.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExternalHttpException}.
 *
 * <p>Callers route on the status code, so the pre-response case must be distinguishable from a
 * real HTTP status rather than defaulting to zero.
 */
class ExternalHttpExceptionTest {

	@Test
	void shouldCarryTheStatusCodeAndBodyOfANonSuccessResponseTest() {
		// Given: a 429 response from a named service
		ExternalHttpException exception =
				new ExternalHttpException("openai returned 429", 429, "{\"error\":\"rate limited\"}");

		// When-Then: the routing details survive intact
		assertThat(exception.getStatusCode()).isEqualTo(429);
		assertThat(exception.getResponseBody()).isEqualTo("{\"error\":\"rate limited\"}");
		assertThat(exception.getMessage()).isEqualTo("openai returned 429");
		assertThat(exception.getCause()).isNull();
	}

	@Test
	void shouldMarkAFailureThatNeverReachedAResponseTest() {
		// Given: a transport failure before any response arrived
		IOException cause = new IOException("connect timed out");
		ExternalHttpException exception = new ExternalHttpException("openai call failed", cause);

		// Then: the status is -1 rather than 0, and the body is empty rather than null
		assertThat(exception.getStatusCode()).isEqualTo(-1);
		assertThat(exception.getResponseBody()).isEmpty();
		assertThat(exception.getCause()).isSameAs(cause);
	}
}
