package com.aidigital.strategyplanning.service.categoryanalysis.services;

/**
 * Reads the client's public website so the draft can be checked against their actual business.
 *
 * <p>Unavailable is a normal outcome, not an error: an invalid, blocked, or non-public URL, a
 * non-200 response, or a network failure all yield {@code null}, which the builder surfaces to
 * the user rather than silently ignoring.
 */
public interface ClientWebsiteReader {

	/**
	 * Fetches the client website and returns its readable text.
	 *
	 * @param clientWebsite client website URL to review
	 * @return truncated readable text of the page, or null when unavailable
	 */
	String fetchWebsiteText(String clientWebsite);
}
