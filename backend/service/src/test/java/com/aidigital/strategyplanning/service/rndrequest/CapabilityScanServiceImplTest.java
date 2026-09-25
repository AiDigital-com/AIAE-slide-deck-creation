package com.aidigital.strategyplanning.service.rndrequest;

import com.aidigital.strategyplanning.service.rndrequest.config.RndRequestProperties;
import com.aidigital.strategyplanning.service.rndrequest.services.impl.CapabilityScanServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CapabilityScanServiceImpl}.
 *
 * <p>The scan is an optional integration: triage must keep working with no Slite or Asana
 * credentials configured. These tests pin that graceful degradation — every source is reported
 * by name with the environment variable that would connect it, and the summary always ends with
 * the manual fallback instead of failing the request.
 */
class CapabilityScanServiceImplTest {

	private static final String TITLE = "Cross-channel pacing alerts";
	private static final String DETAILS = "Growth wants alerts when a campaign overspends.";

	private CapabilityScanServiceImpl service(String sliteKey, String asanaToken) {
		RndRequestProperties properties = new RndRequestProperties();
		properties.setSliteApiKey(sliteKey);
		properties.setAsanaAccessToken(asanaToken);
		return new CapabilityScanServiceImpl(properties);
	}

	@Test
	void shouldReportBothSourcesAsNotConnectedWithoutCredentialsTest() {
		// Given: neither integration is configured
		CapabilityScanServiceImpl service = service("", "");

		// When: a request is scanned
		String summary = service.scan(TITLE, DETAILS);

		// Then: each source names the variable that would connect it, and triage still gets an answer
		assertThat(summary)
				.contains("Slite knowledge base: not connected")
				.contains("SLITE_API_KEY")
				.contains("Asana projects: not connected")
				.contains("ASANA_ACCESS_TOKEN")
				.contains("rely on the manual capability notes");
	}

	@Test
	void shouldReportOnlyTheConfiguredSourceAsConnectedTest() {
		// Given: Slite is configured but Asana is not
		CapabilityScanServiceImpl service = service("slite-key", "");

		// When: a request is scanned
		String summary = service.scan(TITLE, DETAILS);

		// Then: the sources are reported independently rather than as one all-or-nothing state
		assertThat(summary)
				.contains("Slite knowledge base: connected")
				.contains("Asana projects: not connected")
				.doesNotContain("SLITE_API_KEY");
	}

	@Test
	void shouldReportBothSourcesAsConnectedWhenBothCredentialsExistTest() {
		// Given: both integrations are configured
		CapabilityScanServiceImpl service = service("slite-key", "asana-token");

		// When: a request is scanned
		String summary = service.scan(TITLE, DETAILS);

		// Then: no missing-credential hint is shown for either source
		assertThat(summary)
				.contains("Slite knowledge base: connected")
				.contains("Asana projects: connected")
				.doesNotContain("SLITE_API_KEY")
				.doesNotContain("ASANA_ACCESS_TOKEN");
	}

	@Test
	void shouldReturnASummaryWhenTheRequestCarriesNoDetailsTest() {
		// Given: a request with no description, which the contract allows
		CapabilityScanServiceImpl service = service("", "");

		// When: it is scanned
		String summary = service.scan(TITLE, null);

		// Then: the scan still answers instead of failing the triage flow
		assertThat(summary).isNotBlank().contains("rely on the manual capability notes");
	}
}
