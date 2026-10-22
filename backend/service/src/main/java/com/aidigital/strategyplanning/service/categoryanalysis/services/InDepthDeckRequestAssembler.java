package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.Map;

/**
 * Builds the Slides API requests that turn a copy of the In-Depth master into the user's deck:
 * dropping every unchecked slide and filling the tokens of the slides that remain.
 */
public interface InDepthDeckRequestAssembler {

	/**
	 * Builds deleteObject requests for every slide NOT in the checked set, so the copied deck
	 * ends up containing only the slides the user included.
	 *
	 * @param slideIds       slide object ids in master-template order (index 0 = slide 1)
	 * @param selectedSlides slide numbers (1-based) the user checked
	 * @return array of deleteObject requests, one per unchecked slide
	 */
	ArrayNode buildDeleteUnselectedSlidesRequests(List<String> slideIds, List<Integer> selectedSlides);

	/**
	 * Builds replaceAllText requests for the checked slides' tokens. Each token is scoped to
	 * every checked slide it belongs to (a token shared across layout variants is written to each
	 * checked variant), replacing both the {@code {{token}}} form and the master's sample literal
	 * so no placeholder is ever left visible — except the placeholder-only keys, whose literal
	 * fallback is too collision-prone to use safely. Chart data values are never written as text.
	 *
	 * @param tokenValues    final reviewed values keyed by token key
	 * @param slideIds       slide object ids in master-template order (index 0 = slide 1)
	 * @param selectedSlides slide numbers (1-based) the user checked
	 * @return array of Slides API requests
	 */
	ArrayNode buildReplaceTextRequests(Map<String, String> tokenValues, List<String> slideIds,
			List<Integer> selectedSlides);
}
