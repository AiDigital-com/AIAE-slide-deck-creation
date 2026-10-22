package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerFieldBudget;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Reads individual drafted values out of the model's JSON reply: trims them, rejects placeholder
 * answers such as "N/A", records missing fields, and caps every value to its box's budget.
 */
public interface OnePagerJsonFieldReader {

	/**
	 * Parses the model's message content as JSON after stripping a markdown fence or surrounding
	 * commentary.
	 *
	 * @param content raw message content from the model
	 * @return the parsed JSON tree
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the content is
	 * not valid JSON
	 */
	JsonNode parseJson(String content);

	/**
	 * Reads a required text field, recording {@code prefix + field} as missing when it is blank or
	 * a placeholder, otherwise capping it to its budget.
	 *
	 * @param node    JSON object containing the field
	 * @param field   field name
	 * @param budget  character budget of the field
	 * @param missing accumulator of missing field paths
	 * @param prefix  path prefix for nested fields, e.g. {@code "solutions[0]."}, or empty
	 * @return the capped value, or null when missing
	 */
	String requireText(JsonNode node, String field, OnePagerFieldBudget budget, List<String> missing, String prefix);

	/**
	 * Reads a required non-empty string array, capping each entry. Records the field as missing
	 * when the array is absent or empty, and {@code field[]} for each blank or placeholder entry.
	 *
	 * @param node    JSON object containing the field
	 * @param field   name of the array field
	 * @param budget  character budget per entry
	 * @param missing accumulator of missing field paths
	 * @param prefix  path prefix for nested fields, or empty
	 * @return the valid, capped entries
	 */
	List<String> requireStringArray(JsonNode node, String field, OnePagerFieldBudget budget, List<String> missing,
			String prefix);

	/**
	 * Reads an optional string array, dropping blank and placeholder entries and capping the rest.
	 *
	 * @param node   JSON object containing the field
	 * @param field  name of the array field
	 * @param budget character budget per entry
	 * @return the cleaned entries, or an empty list when the field is absent or not an array
	 */
	List<String> optionalStringArray(JsonNode node, String field, OnePagerFieldBudget budget);

	/**
	 * Detects placeholder answers such as "N/A", "TBD", or "-".
	 *
	 * @param value drafted value
	 * @return true when the value is a known placeholder
	 */
	boolean isPlaceholder(String value);

	/**
	 * Truncates a value that exceeds its budget at the last word boundary before the limit.
	 *
	 * @param value  drafted value, may be null
	 * @param budget character budget
	 * @return the value, truncated at a word boundary when it exceeded the budget
	 */
	String capToFit(String value, OnePagerFieldBudget budget);
}
