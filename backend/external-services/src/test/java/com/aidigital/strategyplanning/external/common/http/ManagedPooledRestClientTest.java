package com.aidigital.strategyplanning.external.common.http;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ManagedPooledRestClient}.
 *
 * <p>Shutdown is best-effort by design: a pool must still be released when the HTTP client
 * refuses to close, otherwise a failing client would leak its connections on every redeploy.
 */
class ManagedPooledRestClientTest {

	@Test
	void shouldExposeTheNameAndClientItWasBuiltWithTest() {
		// Given: a managed client for one logical service
		RestClient restClient = RestClient.builder().baseUrl("https://example.test").build();
		ManagedPooledRestClient managed = new ManagedPooledRestClient("openai", restClient,
				mock(CloseableHttpClient.class), mock(PoolingHttpClientConnectionManager.class));

		// When-Then: both accessors return what was supplied
		assertThat(managed.name()).isEqualTo("openai");
		assertThat(managed.restClient()).isSameAs(restClient);
	}

	@Test
	void shouldCloseBothTheHttpClientAndTheConnectionManagerTest() throws IOException {
		// Given: a managed client whose resources close cleanly
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
		PoolingHttpClientConnectionManager connectionManager =
				mock(PoolingHttpClientConnectionManager.class);
		ManagedPooledRestClient managed = new ManagedPooledRestClient("openai",
				RestClient.builder().baseUrl("https://example.test").build(),
				httpClient, connectionManager);

		// When: the client is closed
		managed.close();

		// Then: both pooled resources are released
		verify(httpClient).close();
		verify(connectionManager).close();
	}

	@Test
	void shouldStillReleaseThePoolWhenTheHttpClientFailsToCloseTest() throws IOException {
		// Given: an HTTP client that throws on close
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
		doThrow(new IOException("socket already gone")).when(httpClient).close();
		PoolingHttpClientConnectionManager connectionManager =
				mock(PoolingHttpClientConnectionManager.class);
		ManagedPooledRestClient managed = new ManagedPooledRestClient("openai",
				RestClient.builder().baseUrl("https://example.test").build(),
				httpClient, connectionManager);

		// When-Then: shutdown swallows the failure and still closes the connection manager
		assertThatCode(managed::close).doesNotThrowAnyException();
		verify(connectionManager).close();
	}
}
