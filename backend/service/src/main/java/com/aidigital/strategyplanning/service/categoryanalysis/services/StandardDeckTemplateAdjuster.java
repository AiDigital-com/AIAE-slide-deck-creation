package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Builds the corrections applied to a freshly copied Standard deck.
 *
 * <p>The template ships with sample artwork and sample copy that the token replacement cannot
 * remove: a duplicate image behind the circular crop, a chart title, a section label, and headings
 * whose styling has to match the brand. Every correction is best-effort — an element the template
 * no longer contains is simply skipped, so a template edit never fails deck creation.
 */
public interface StandardDeckTemplateAdjuster {

	/**
	 * Builds the batchUpdate requests that correct a copied deck.
	 *
	 * @param presentation parsed presentation JSON, as copied and before adjustments
	 * @return requests to send, empty when nothing needs correcting
	 */
	ArrayNode buildAdjustmentRequests(JsonNode presentation);
}
