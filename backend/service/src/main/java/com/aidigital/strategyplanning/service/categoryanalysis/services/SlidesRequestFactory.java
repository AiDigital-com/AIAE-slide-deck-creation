package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.SourceLink;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

/**
 * Builds the Google Slides batchUpdate requests the deck creation flow sends.
 *
 * <p>Request shapes are provider contract rather than product decisions, but they carry product
 * intent — keeping a citation's own colour instead of the API's blue hyperlink default, placing
 * artwork at fixed offsets so the template's composition survives — so they are kept together and
 * tested directly.
 */
public interface SlidesRequestFactory {

	/**
	 * Builds updateTextStyle requests that hyperlink each resolved citation.
	 *
	 * @param presentation parsed presentation JSON, fetched after the text replacement
	 * @param sourceValues final sources text keyed by token key
	 * @param linksByKey   resolved links keyed by token key
	 * @return array of Slides API requests, empty when no citation could be located
	 */
	ArrayNode buildSourceLinkRequests(JsonNode presentation, Map<String, String> sourceValues,
			Map<String, List<SourceLink>> linksByKey);

	/**
	 * Builds one updateTextStyle request that turns a text range into a link.
	 *
	 * @param objectId        shape object id
	 * @param startIndex      first character of the range
	 * @param endIndex        one past the last character of the range
	 * @param url             link target
	 * @param foregroundColor colour to keep, or null to leave the colour alone
	 * @return request node
	 */
	ObjectNode updateTextLinkRequest(String objectId, int startIndex, int endIndex, String url,
			JsonNode foregroundColor);

	/**
	 * Builds the replaceAllText requests that fill every template token.
	 *
	 * @param tokenValues reviewed values keyed by token key
	 * @param slideIds    slide object ids to scope the replacement to
	 * @return array of Slides API requests
	 */
	ArrayNode buildReplaceTextRequests(Map<String, String> tokenValues, List<String> slideIds);

	/**
	 * Builds one replaceAllText request.
	 *
	 * @param find    text to find
	 * @param replace replacement text
	 * @param pageId  slide to scope the replacement to
	 * @return request node
	 */
	ObjectNode replaceAllTextRequest(String find, String replace, String pageId);

	/**
	 * Builds a request that places an element at a fixed size and position.
	 *
	 * @param element    page element to move
	 * @param targetSize target size (EMU)
	 * @param translateX absolute X position (EMU)
	 * @param translateY absolute Y position (EMU)
	 * @return request node
	 */
	ObjectNode buildAbsolutePlacementRequest(JsonNode element, double targetSize, double translateX,
			double translateY);

	/**
	 * Builds a request that adds a generated image to a slide.
	 *
	 * @param slideObjectId slide to add the image to
	 * @param imageUrl      fetchable URL of the generated image
	 * @return request node
	 */
	ObjectNode buildCreateImageRequest(String slideObjectId, String imageUrl);
}
