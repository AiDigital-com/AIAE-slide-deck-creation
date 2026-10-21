package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;

/**
 * Maps the model's JSON reply onto a {@link OnePagerDraft}, reading only the fields of the picked
 * modules, capping each value to its budget, and refusing an incomplete draft.
 */
public interface OnePagerDraftParser {

	/**
	 * Parses a drafting reply.
	 *
	 * @param content raw message content returned by the model
	 * @param brief   the brief the draft was made for; decides which fields are required
	 * @return the drafted page content
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the reply is not
	 * valid JSON or leaves a required field blank or a placeholder
	 */
	OnePagerDraft parseDraft(String content, OnePagerBrief brief);
}
