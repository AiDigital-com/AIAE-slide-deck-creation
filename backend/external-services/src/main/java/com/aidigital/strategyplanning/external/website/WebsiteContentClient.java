package com.aidigital.strategyplanning.external.website;

/**
 * Fetches a public web page on behalf of a feature that enriches a prompt with the client's own
 * site.
 *
 * <p>Every failure resolves to null: the page is an optional prompt input, so an unreachable or
 * refused site must degrade the draft rather than fail the request.
 */
public interface WebsiteContentClient {

	/**
	 * Fetches the raw HTML of a public page.
	 *
	 * @param url page to fetch
	 * @return the response body, or null when the URL is not fetchable or the call did not succeed
	 */
	String fetchHtml(String url);
}
