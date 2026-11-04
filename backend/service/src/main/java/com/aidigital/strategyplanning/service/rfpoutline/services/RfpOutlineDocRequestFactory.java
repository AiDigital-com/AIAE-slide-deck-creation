package com.aidigital.strategyplanning.service.rfpoutline.services;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;

/**
 * Builds the Google Docs batchUpdate requests that write a reviewed RFP outline into a blank
 * document: the title as the top heading, then every non-blank section under its own heading.
 */
public interface RfpOutlineDocRequestFactory {

	/**
	 * Builds the requests for one outline.
	 *
	 * @param title         document title, written as the first heading
	 * @param sectionValues section text keyed by {@link
	 *                      com.aidigital.strategyplanning.service.rfpoutline.enums.RfpOutlineSection#getKey()};
	 *                      blank or missing sections are left out
	 * @return requests in the order the Docs API must apply them
	 */
	ArrayNode buildDocumentRequests(String title, Map<String, String> sectionValues);
}
