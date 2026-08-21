package com.aidigital.strategyplanning.service.rndrequest.services.impl;

import com.aidigital.strategyplanning.service.rndrequest.config.RndRequestProperties;
import com.aidigital.strategyplanning.service.rndrequest.services.CapabilityScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link CapabilityScanService}.
 * Live Slite/Asana search is activated when the corresponding credentials are
 * configured; until then each source is reported as not connected so the
 * triage flow keeps working without external keys.
 */
@Service
@RequiredArgsConstructor
public class CapabilityScanServiceImpl implements CapabilityScanService {

	private final RndRequestProperties rndRequestProperties;

	@Override
	public String scan(String title, String requestDetails) {
		boolean sliteConnected = StringUtils.hasText(rndRequestProperties.getSliteApiKey());
		boolean asanaConnected = StringUtils.hasText(rndRequestProperties.getAsanaAccessToken());
		StringBuilder summary = new StringBuilder();
		summary.append("Slite knowledge base: ")
				.append(sliteConnected
						? "connected — automatic capability search pending engineering wiring."
						: "not connected — add SLITE_API_KEY to enable automatic capability search.")
				.append('\n');
		summary.append("Asana projects: ")
				.append(asanaConnected
						? "connected — automatic capability search pending engineering wiring."
						: "not connected — add ASANA_ACCESS_TOKEN to enable automatic capability search.")
				.append('\n');
		summary.append("Until sources are connected, rely on the manual capability notes captured with the request.");
		return summary.toString();
	}
}
