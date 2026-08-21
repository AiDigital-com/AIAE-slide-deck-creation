package com.aidigital.strategyplanning.external.website;

/**
 * Decides whether a user-supplied URL is safe for the server to fetch.
 *
 * <p>This is the server-side request forgery guard: without it, a user could point the builder at
 * an internal address and use the server as a probe into the private network.
 */
public interface PublicHttpAddressPolicy {

	/**
	 * Reports whether a URL is a public http(s) address safe to fetch server-side.
	 *
	 * @param url candidate URL
	 * @return true when the URL is http(s) and resolves only to publicly routable addresses
	 */
	boolean isFetchableUrl(String url);
}
