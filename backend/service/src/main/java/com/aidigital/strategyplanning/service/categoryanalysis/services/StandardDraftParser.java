package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;

/**
 * Reads the model's JSON replies into Standard deck drafts.
 *
 * <p>A reply that omits template fields is rejected rather than partially applied: a deck with
 * blank placeholders is worse than a visible failure.
 */
public interface StandardDraftParser {

	/**
	 * Parses a full-deck reply.
	 *
	 * @param content JSON object string returned by the model
	 * @return parsed draft with its alignment verdict
	 */
	StandardDraft parseDraft(String content);

	/**
	 * Parses a single-slide redraft reply.
	 *
	 * @param content     JSON object string returned by the model
	 * @param slideNumber slide the redraft belongs to
	 * @return parsed draft carrying only that slide's fields
	 */
	StandardDraft parseSlideDraft(String content, int slideNumber);
}
