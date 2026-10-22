package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * A reviewed field value keyed by its In-Depth template token.
 *
 * @param key   stable template token key
 * @param value final reviewed text value
 */
public record InDepthFieldValue(
		String key,
		String value
) {

}
