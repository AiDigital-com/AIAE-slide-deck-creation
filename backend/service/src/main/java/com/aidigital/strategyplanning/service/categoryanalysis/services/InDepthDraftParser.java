package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.DraftAlignment;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Reads the model's replies into In-Depth deck drafts.
 *
 * <p>Not every call runs in strict JSON mode — the {@code web_search} tool cannot — so the parser
 * tolerates a markdown fence or stray commentary around the JSON object. A reply that omits a
 * requested field, or answers it with a placeholder, is rejected rather than partially applied:
 * a deck with blank or "N/A" boxes is worse than a visible failure.
 */
public interface InDepthDraftParser {

	/**
	 * Parses a full-deck reply carrying both fields and alignment.
	 *
	 * @param content model output text
	 * @param tokens  token specs that were requested (defines the required key set)
	 * @return parsed draft with its alignment verdict
	 */
	InDepthDraft parseDraft(String content, List<InDepthTokenSpec> tokens);

	/**
	 * Parses a single-slide redraft reply.
	 *
	 * @param content     model output text
	 * @param slideTokens token specs belonging to the redrafted slide
	 * @return parsed draft holding only that slide's fields, with a null alignment
	 */
	InDepthDraft parseSlideDraft(String content, List<InDepthTokenSpec> slideTokens);

	/**
	 * Parses model output into JSON, isolating the JSON object first.
	 *
	 * @param content model output text
	 * @return parsed JSON root
	 */
	JsonNode parseJson(String content);

	/**
	 * Extracts and validates the drafted field values for the given token specs.
	 *
	 * @param root   parsed JSON root of the model response
	 * @param tokens token specs that were requested
	 * @return drafted fields, formatted and capped to fit, in token registry order
	 */
	List<InDepthDraftField> parseFields(JsonNode root, List<InDepthTokenSpec> tokens);

	/**
	 * Reads the fact-check reply for one topic group. A key is absent from the result when the
	 * model returned a placeholder for it, so the field keeps its drafted value.
	 *
	 * @param root  parsed JSON root of the fact-check response
	 * @param group the topic's factual token specs that were verified
	 * @return corrected values keyed by token key, raw (not yet formatted)
	 */
	Map<String, String> parseCorrections(JsonNode root, List<InDepthTokenSpec> group);

	/**
	 * Reads the website-based alignment fields from the model output, tolerating omissions.
	 *
	 * @param root parsed JSON root of the model response
	 * @return confirmed alignment result, defaulting to a matched state when the model omits it
	 */
	DraftAlignment parseAlignment(JsonNode root);
}
