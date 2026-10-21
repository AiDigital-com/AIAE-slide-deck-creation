package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;

import java.util.List;
import java.util.Map;

/**
 * The measure-and-revise loop that makes every drafted One-Page Case Study value fit its box.
 *
 * <p>Values that leave their box partly empty or run past it go back to the model with exact
 * feedback ("fills 3 of 6 lines — add about 150 characters"), for a bounded number of rounds; a
 * rewrite is kept only when it measures closer to its box. Anything still too long is cut at a
 * word boundary so nothing overflows; anything still short is kept and flagged for the reviewer —
 * the AI never pads with invented facts.
 */
public interface OnePageFitRevisionPolicy {

	/**
	 * Revises the values in place until every measured field fits its box or the round budget
	 * runs out, then trims anything still overflowing.
	 *
	 * @param values    current values keyed by field, updated in place
	 * @param documents parsed text documents
	 * @param images    uploaded images
	 */
	void reviseToFit(Map<String, String> values, List<SourceDocument> documents, List<SourceImage> images);
}
