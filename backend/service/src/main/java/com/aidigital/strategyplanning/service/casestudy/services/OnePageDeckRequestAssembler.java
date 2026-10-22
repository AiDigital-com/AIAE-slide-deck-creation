package com.aidigital.strategyplanning.service.casestudy.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;

/**
 * Builds the Slides API requests that swap the One-Page Case Study template's tagged icons for
 * the chosen library icons.
 */
public interface OnePageDeckRequestAssembler {

	/**
	 * Builds replaceImage requests for every image (searching inside groups) whose alt text is
	 * {@code {{slot}}} for a slot in the map. The replacement keeps the image's position, size,
	 * and layer.
	 *
	 * @param presentation    presentation JSON as the Slides API returns it
	 * @param imageUrlsBySlot public image URLs keyed by slot (alt text without braces)
	 * @return array of Slides API replaceImage requests, empty when nothing matches
	 */
	ArrayNode buildReplaceImageRequests(JsonNode presentation, Map<String, String> imageUrlsBySlot);
}
