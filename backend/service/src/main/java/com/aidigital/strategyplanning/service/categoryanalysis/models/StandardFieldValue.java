package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * A reviewed field value keyed by its Standard template token.
 *
 * @param key   stable template token key
 * @param value final reviewed text value
 */
public record StandardFieldValue(
		String key,
		String value
) {

}
