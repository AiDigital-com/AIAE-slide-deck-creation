package com.aidigital.strategyplanning.external.website.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.website.PublicHttpAddressPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebsiteContentClientImpl}.
 *
 * <p>The website is an optional prompt input, so every unusable outcome must resolve to null; and
 * a URL the address policy refuses must never reach the network at all.
 */
class WebsiteContentClientImplTest {

	@Test
	void shouldNotConnectToAUrlTheAddressPolicyRefusesTest() {
		// Given: a URL the server-side request forgery guard rejects
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		PublicHttpAddressPolicy addressPolicy = mock(PublicHttpAddressPolicy.class);
		when(addressPolicy.isFetchableUrl("http://169.254.169.254")).thenReturn(false);

		// When: the fetch is attempted
		String html = new WebsiteContentClientImpl(factory, addressPolicy)
				.fetchHtml("http://169.254.169.254");

		// Then: no client is even built, so nothing probes the internal network
		assertThat(html).isNull();
		verifyNoInteractions(factory);
	}

	@Test
	void shouldReturnNullWhenTheFetchFailsTest() {
		// Given: an allowed URL but a client whose call raises
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		PublicHttpAddressPolicy addressPolicy = mock(PublicHttpAddressPolicy.class);
		when(addressPolicy.isFetchableUrl("https://example.com")).thenReturn(true);
		when(factory.createAbsoluteUrlClient("client-website", Duration.ofSeconds(15)))
				.thenReturn(RestClient.create());

		// When-Then: an unreachable site degrades the draft instead of failing the request
		assertThat(new WebsiteContentClientImpl(factory, addressPolicy)
				.fetchHtml("https://example.invalid/never-resolves")).isNull();
	}

	@Test
	void shouldBuildThePoolOnceAndReuseItTest() {
		// Given: a client asked for repeatedly
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		PublicHttpAddressPolicy addressPolicy = mock(PublicHttpAddressPolicy.class);
		when(factory.createAbsoluteUrlClient("client-website", Duration.ofSeconds(15)))
				.thenReturn(RestClient.create());
		WebsiteContentClientImpl client = new WebsiteContentClientImpl(factory, addressPolicy);

		// When: the pooled client is resolved twice
		RestClient first = client.client();
		RestClient second = client.client();

		// Then: one pool serves every site, so visiting many hosts cannot grow the pool count
		assertThat(first).isSameAs(second);
		verify(factory, times(1)).createAbsoluteUrlClient("client-website", Duration.ofSeconds(15));
	}
}
