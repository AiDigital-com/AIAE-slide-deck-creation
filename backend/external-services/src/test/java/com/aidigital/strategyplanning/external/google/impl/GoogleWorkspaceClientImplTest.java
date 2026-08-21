package com.aidigital.strategyplanning.external.google.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.model.GoogleApiFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the response handling of {@link GoogleWorkspaceClientImpl}.
 */
class GoogleWorkspaceClientImplTest {

	private GoogleWorkspaceClientImpl client() {
		return new GoogleWorkspaceClientImpl(mock(PooledRestClientFactory.class), new ObjectMapper());
	}

	@Test
	void shouldParseASuccessfulResponseTest() {
		// Given: a Drive copy response
		// When-Then: the tree is returned as-is for the caller to read
		assertThat(client().parse("{\"id\":\"deck123\"}").path("id").asText()).isEqualTo("deck123");
	}

	@Test
	void shouldReportATransportFailureWhenTheBodyIsNotJsonTest() {
		// Given: an HTML error page instead of JSON
		// When-Then: it is a transport failure, not a silent empty tree
		assertThatThrownBy(() -> client().parse("<html>502</html>"))
				.isInstanceOf(GoogleApiException.class)
				.extracting(e -> ((GoogleApiException) e).getFailure())
				.isEqualTo(GoogleApiFailure.TRANSPORT);
	}

	@Test
	void shouldReportAnInterruptedCallDistinctlyTest() {
		// Given: the calling thread was interrupted while waiting
		GoogleWorkspaceClientImpl client = client();
		Thread.currentThread().interrupt();
		try {
			// When: the transport failure is mapped
			GoogleApiException failure =
					client.transportFailure(new RuntimeException("socket closed"));

			// Then: the interruption is preserved rather than reported as a plain failure
			assertThat(failure.getFailure()).isEqualTo(GoogleApiFailure.INTERRUPTED);
			assertThat(failure.getStatusCode()).isEqualTo(-1);
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void shouldReportAPlainTransportFailureWhenNotInterruptedTest() {
		// Given: an uninterrupted thread
		// When-Then: the failure is transport, carrying the cause's message
		GoogleApiException failure = client().transportFailure(new RuntimeException("connect reset"));
		assertThat(failure.getFailure()).isEqualTo(GoogleApiFailure.TRANSPORT);
		assertThat(failure.getMessage()).isEqualTo("connect reset");
	}

	@Test
	void shouldReuseOnePooledClientPerApiHostTest() {
		// Given: a factory that answers with a client
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		org.mockito.Mockito.when(factory.createClient(
						org.mockito.ArgumentMatchers.anyString(),
						org.mockito.ArgumentMatchers.anyString(),
						org.mockito.ArgumentMatchers.any(java.time.Duration.class)))
				.thenReturn(org.springframework.web.client.RestClient.create());
		GoogleWorkspaceClientImpl client = new GoogleWorkspaceClientImpl(factory, new ObjectMapper());

		// When: the same host is asked for twice
		client.client("https://slides.googleapis.com/v1", "google-slides");
		client.client("https://slides.googleapis.com/v1", "google-slides");

		// Then: Drive and Slides get separate pools, but each host only one
		org.mockito.Mockito.verify(factory).createClient(
				org.mockito.ArgumentMatchers.eq("google-slides"),
				org.mockito.ArgumentMatchers.eq("https://slides.googleapis.com/v1"),
				org.mockito.ArgumentMatchers.any(java.time.Duration.class));
		org.mockito.Mockito.verifyNoMoreInteractions(factory);
	}
}
