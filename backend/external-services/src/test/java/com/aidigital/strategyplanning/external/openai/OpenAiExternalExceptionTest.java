package com.aidigital.strategyplanning.external.openai;

import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenAiExternalException}.
 *
 * <p>Callers log the provider's own error text on an HTTP failure, so the body must survive; a
 * transport failure never had a response and must be distinguishable from a real status.
 */
class OpenAiExternalExceptionTest {

	@Test
	void shouldCarryStatusAndBodyOfAnHttpFailureTest() {
		// Given: a 429 with the provider's error text
		OpenAiExternalException exception = new OpenAiExternalException(
				OpenAiFailure.HTTP_STATUS, 429, "returned HTTP 429", "{\"error\":\"slow down\"}");

		// When-Then: everything a caller logs or routes on survives
		assertThat(exception.getFailure()).isEqualTo(OpenAiFailure.HTTP_STATUS);
		assertThat(exception.getStatusCode()).isEqualTo(429);
		assertThat(exception.getResponseBody()).isEqualTo("{\"error\":\"slow down\"}");
	}

	@Test
	void shouldDefaultTheBodyToEmptyWhenNotSuppliedTest() {
		// Given: an HTTP failure constructed without a body
		OpenAiExternalException exception =
				new OpenAiExternalException(OpenAiFailure.EMPTY_CONTENT, 200, "no content");

		// When-Then: the body is empty rather than null
		assertThat(exception.getResponseBody()).isEmpty();
	}

	@Test
	void shouldMarkAFailureThatNeverReachedAResponseTest() {
		// Given: a transport failure
		IOException cause = new IOException("connect timed out");
		OpenAiExternalException exception =
				new OpenAiExternalException(OpenAiFailure.TRANSPORT, "call failed", cause);

		// Then: the status is -1 rather than 0, and the cause is preserved
		assertThat(exception.getStatusCode()).isEqualTo(-1);
		assertThat(exception.getResponseBody()).isEmpty();
		assertThat(exception.getCause()).isSameAs(cause);
	}
}
