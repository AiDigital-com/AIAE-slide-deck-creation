package com.aidigital.strategyplanning.external.google.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.config.GoogleWorkspaceClientProperties;
import com.aidigital.strategyplanning.external.google.model.GoogleApiFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the transient-retry and multipart helpers of {@link GoogleWorkspaceClientImpl}.
 */
class GoogleWorkspaceClientRetryTest {

	private GoogleWorkspaceClientImpl client(int attempts) {
		GoogleWorkspaceClientProperties properties = new GoogleWorkspaceClientProperties();
		properties.setTransientRetryAttempts(attempts);
		properties.setTransientRetryBackoff(Duration.ofMillis(1));
		return new GoogleWorkspaceClientImpl(mock(PooledRestClientFactory.class), new ObjectMapper(),
				properties);
	}

	@Test
	void shouldReturnTheFirstSuccessfulAttemptTest() {
		// Given: a call that succeeds at once
		AtomicInteger calls = new AtomicInteger();

		// When: it is run with retries available
		String result = client(3).withTransientRetry("export", () -> {
			calls.incrementAndGet();
			return "ok";
		});

		// Then: it ran exactly once
		assertThat(result).isEqualTo("ok");
		assertThat(calls.get()).isEqualTo(1);
	}

	@Test
	void shouldRunACallOnlyOnceWhenASingleAttemptIsConfiguredTest() {
		// Given: a call that fails with a 503 and one attempt allowed
		AtomicInteger calls = new AtomicInteger();

		// When-Then: the failure propagates without any resend
		assertThatThrownBy(() -> client(1).withTransientRetry("export", () -> {
			calls.incrementAndGet();
			throw new GoogleApiException(GoogleApiFailure.HTTP_STATUS, 503, "error", "HTTP 503");
		}))
				.isInstanceOf(GoogleApiException.class);
		assertThat(calls.get()).isEqualTo(1);
	}

	@Test
	void shouldNotResendATransportFailureTest() {
		// Given: a call that fails before any response
		AtomicInteger calls = new AtomicInteger();

		// When-Then: a transport failure is not a 5xx and is never resent
		assertThatThrownBy(() -> client(3).withTransientRetry("export", () -> {
			calls.incrementAndGet();
			throw new GoogleApiException(GoogleApiFailure.TRANSPORT, "reset", new RuntimeException());
		}))
				.isInstanceOf(GoogleApiException.class)
				.satisfies(e -> assertThat(((GoogleApiException) e).getFailure())
						.isEqualTo(GoogleApiFailure.TRANSPORT));
		assertThat(calls.get()).isEqualTo(1);
	}

	@Test
	void shouldClassifyOnlyServerErrorsAsTransientTest() {
		// Given: a client with default settings
		GoogleWorkspaceClientImpl client = client(3);

		// When-Then: 5xx statuses are transient, rejections and transport failures are not
		assertThat(client.isTransient(new GoogleApiException(GoogleApiFailure.HTTP_STATUS, 500, "", "")))
				.isTrue();
		assertThat(client.isTransient(new GoogleApiException(GoogleApiFailure.HTTP_STATUS, 503, "", "")))
				.isTrue();
		assertThat(client.isTransient(new GoogleApiException(GoogleApiFailure.HTTP_STATUS, 499, "", "")))
				.isFalse();
		assertThat(client.isTransient(new GoogleApiException(GoogleApiFailure.TRANSPORT, "x",
				new RuntimeException()))).isFalse();
	}

	@Test
	void shouldReportAnInterruptedBackOffDistinctlyTest() {
		// Given: the calling thread is interrupted before the pause
		GoogleWorkspaceClientImpl client = client(3);
		Thread.currentThread().interrupt();
		try {
			// When-Then: the pause ends at once as an interruption, with the flag restored
			assertThatThrownBy(() -> client.backOff(1))
					.isInstanceOf(GoogleApiException.class)
					.satisfies(e -> assertThat(((GoogleApiException) e).getFailure())
							.isEqualTo(GoogleApiFailure.INTERRUPTED));
			assertThat(Thread.currentThread().isInterrupted()).isTrue();
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void shouldBuildTheMultipartBodyInDrivesOrderTest() {
		// Given: metadata, a source type and some bytes
		byte[] content = {0x01, 0x02};

		// When: the body is built
		byte[] body = client(3).multipartBody("b", "{\"name\":\"x\"}", "application/pptx", content);

		// Then: metadata part, media part, then the closing delimiter, each CRLF-separated
		assertThat(new String(body, StandardCharsets.ISO_8859_1)).isEqualTo(
				"--b\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n{\"name\":\"x\"}\r\n"
						+ "--b\r\nContent-Type: application/pptx\r\n\r\n\u0001\u0002\r\n--b--");
	}
}
