package com.aidigital.strategyplanning.external.website.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.website.PublicHttpAddressPolicy;
import com.aidigital.strategyplanning.external.website.WebsiteContentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pooled implementation of {@link WebsiteContentClient}.
 *
 * <p>The target host is whatever the user typed, so this uses a single pool addressed by absolute
 * URI rather than one pool per site.
 *
 * <p>Every {@code exchange} call passes {@code close = true}. Spring closes the response only in
 * that case, and each handler here reads the whole body into memory before returning, so there is
 * nothing left to stream. With {@code false} the pooled connection is never released and the pool
 * starves after {@code max-connections-per-route} calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebsiteContentClientImpl implements WebsiteContentClient {

	private static final String CLIENT_NAME = "client-website";
	private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(15);
	private static final String USER_AGENT = "Mozilla/5.0 (compatible; StrategyPlanningBot/1.0)";

	private final PooledRestClientFactory restClientFactory;
	private final PublicHttpAddressPolicy addressPolicy;
	private final AtomicReference<RestClient> client = new AtomicReference<>();

	@Override
	public String fetchHtml(String url) {
		if (!addressPolicy.isFetchableUrl(url)) {
			return null;
		}
		try {
			return client()
					.get()
					.uri(URI.create(url.trim()))
					.header(HttpHeaders.USER_AGENT, USER_AGENT)
					.exchange((request, response) -> response.getStatusCode().value() == 200
							? new String(response.getBody().readAllBytes())
							: null, true);
		} catch (RuntimeException e) {
			log.warn("Client website fetch failed: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Returns the pooled client, creating it on first use.
	 *
	 * @return pooled REST client that takes absolute request URIs
	 */
	RestClient client() {
		RestClient existing = client.get();
		if (existing != null) {
			return existing;
		}
		RestClient created = restClientFactory.createAbsoluteUrlClient(CLIENT_NAME, FETCH_TIMEOUT);
		return client.compareAndSet(null, created) ? created : client.get();
	}
}
