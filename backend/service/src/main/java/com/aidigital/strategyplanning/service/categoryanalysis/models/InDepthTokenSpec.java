package com.aidigital.strategyplanning.service.categoryanalysis.models;

import java.util.List;

/**
 * One token definition of the In-Depth Category Analysis template.
 *
 * @param key          stable token key (e.g. "trend_headline")
 * @param label        human-readable label for the review UI
 * @param slideNumbers every slide this token's value is written to when that slide is checked
 *                     (more than one entry for content shared across layout variants of the
 *                     same topic)
 * @param sampleText   literal sample text currently in the master template; every slide in
 *                     {@code slideNumbers} carries this same literal at authoring time
 */
public record InDepthTokenSpec(
		String key,
		String label,
		List<Integer> slideNumbers,
		String sampleText
) {

}
