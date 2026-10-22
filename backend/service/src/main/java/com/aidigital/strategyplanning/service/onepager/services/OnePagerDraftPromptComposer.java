package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;

/**
 * Builds the chat completions request that drafts a one-pager from a brief. The prompt lists only
 * the fields of the picked modules, each with its character budget, and asks for strict JSON.
 */
public interface OnePagerDraftPromptComposer {

	/**
	 * Builds the drafting request body.
	 *
	 * @param brief the brief and the modules to draft
	 * @return serialized chat completions request body
	 */
	String buildDraftRequestBody(OnePagerBrief brief);
}
