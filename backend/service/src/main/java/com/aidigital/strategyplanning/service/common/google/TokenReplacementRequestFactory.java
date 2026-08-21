package com.aidigital.strategyplanning.service.common.google;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;

/**
 * Builds the {@code replaceAllText} requests that fill a tokenized Google master template.
 *
 * <p>The Slides and Docs batchUpdate APIs accept the same request shape, so the case study deck
 * and the RFP outline doc share this factory.
 */
public interface TokenReplacementRequestFactory {

	/**
	 * Builds one replaceAllText request per token value, matching the {@code {{token}}} form.
	 *
	 * @param tokenValues token values keyed by token key (without braces)
	 * @return array of replaceAllText requests
	 */
	ArrayNode buildReplaceTextRequests(Map<String, String> tokenValues);
}
