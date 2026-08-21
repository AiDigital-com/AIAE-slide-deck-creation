package com.aidigital.strategyplanning.service.rndrequest.services;

/**
 * Service contract for scanning connected knowledge sources (Slite, Asana)
 * for existing capabilities matching an RnD request.
 * Implementations must degrade gracefully when integrations are not connected.
 */
public interface CapabilityScanService {

	/**
	 * Scans connected knowledge sources for capabilities relevant to the request
	 * and returns a human-readable summary. When a source is not connected, the
	 * summary explicitly reports it as "not connected" instead of failing.
	 *
	 * @param title          short title of the requested capability
	 * @param requestDetails full request description, may be null
	 * @return human-readable capability search summary, never null
	 */
	String scan(String title, String requestDetails);
}
