package com.aidigital.strategyplanning.service.categoryanalysis.models;

import java.util.List;

/**
 * One drafted In-Depth Category Analysis template field, written to every checked slide that
 * shares it.
 *
 * @param key          stable template token key (e.g. "trend_headline")
 * @param label        human-readable label shown in the review UI
 * @param slideNumbers every slide in the master template this field belongs to (more than one
 *                     entry when the field is shared across layout variants of the same topic)
 * @param value        drafted text value
 */
public record InDepthDraftField(
		String key,
		String label,
		List<Integer> slideNumbers,
		String value
) {

}
