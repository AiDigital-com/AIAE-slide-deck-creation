package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Reads the model's JSON reply into the shapes the Case Study Builder works with.
 *
 * <p>Parsing is deliberately tolerant: the model may omit a field, send {@code null}, or send
 * whitespace, and all three mean "no value" rather than an empty string on the slide.
 */
public interface CaseStudyDraftParser {

	/**
	 * Parses the model's JSON content into a draft.
	 *
	 * @param content         JSON object string returned by the model
	 * @param sourceFileNames comma-separated names of the source documents used
	 * @return parsed draft
	 */
	CaseStudyDraft parseDraft(String content, String sourceFileNames);

	/**
	 * Reads a string property from a JSON node, returning null when missing, null, or blank.
	 *
	 * @param root parsed JSON object
	 * @param key  property name to read
	 * @return trimmed string value, or null when absent
	 */
	String textOrNull(JsonNode root, String key);
}
