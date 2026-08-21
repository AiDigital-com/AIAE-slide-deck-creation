package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * One drafted Standard Category Analysis template field with slide grouping metadata.
 *
 * @param key         stable template token key (e.g. "trends_headline")
 * @param label       human-readable label shown in the review UI
 * @param slideNumber slide the field belongs to (1-5)
 * @param value       drafted text value
 */
public record StandardDraftField(
		String key,
		String label,
		int slideNumber,
		String value
) {

}
