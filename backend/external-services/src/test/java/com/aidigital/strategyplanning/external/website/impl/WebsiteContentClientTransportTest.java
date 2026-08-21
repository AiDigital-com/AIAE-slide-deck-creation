package com.aidigital.strategyplanning.external.website.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.website.PublicHttpAddressPolicy;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Transport-level tests for {@link WebsiteContentClientImpl} against a stub HTTP server.
 *
 * <p>The address policy is stubbed to allow the stub server, which is on loopback and the real
 * guard would rightly refuse. What is under test here is the fetch itself: the page is optional
 * prompt material, so anything other than a 200 has to come back as null.
 */
class WebsiteContentClientTransportTest {

	/**
	 * Builds a client that trusts every URL and points at the stub server.
	 *
	 * @param server stub HTTP server
	 * @return client under test
	 */
	private WebsiteContentClientImpl clientFor(MockWebServer server) {
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		when(factory.createAbsoluteUrlClient("client-website", Duration.ofSeconds(15)))
				.thenReturn(RestClient.create());
		PublicHttpAddressPolicy policy = mock(PublicHttpAddressPolicy.class);
		when(policy.isFetchableUrl(anyString())).thenReturn(true);
		return new WebsiteContentClientImpl(factory, policy);
	}

	@Test
	void shouldReturnThePageBodyAndIdentifyItselfTest() throws Exception {
		// Given: the client's site serves a page
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200)
					.setBody("<html><body>We sell boots</body></html>"));

			// When: the page is fetched by absolute URL
			String html = clientFor(server).fetchHtml(server.url("/about").toString());

			// Then: the raw HTML comes back and the request says who is asking
			assertThat(html).contains("We sell boots");
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("GET");
			assertThat(request.getPath()).isEqualTo("/about");
			assertThat(request.getHeader(HttpHeaders.USER_AGENT))
					.isEqualTo("Mozilla/5.0 (compatible; StrategyPlanningBot/1.0)");
		}
	}

	@Test
	void shouldTrimSurroundingWhitespaceFromThePastedUrlTest() throws Exception {
		// Given: a URL the user pasted with stray whitespace
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

			// When: it is fetched as typed
			String html = clientFor(server).fetchHtml("  " + server.url("/") + "  ");

			// Then: the fetch still succeeds instead of failing on an unparseable URI
			assertThat(html).isEqualTo("ok");
		}
	}

	@Test
	void shouldReturnNullForANonOkStatusTest() throws IOException {
		// Given: the site answers with a redirect-to-login or a not-found
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			server.enqueue(new MockResponse().setResponseCode(404).setBody("nope"));

			// When-Then: only a 200 counts as usable prompt material
			assertThat(clientFor(server).fetchHtml(server.url("/missing").toString())).isNull();
		}
	}
}
