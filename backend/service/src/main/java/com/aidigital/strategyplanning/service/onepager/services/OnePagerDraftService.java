package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;

/**
 * AI drafting for the One Page Generator.
 *
 * <p>Unlike the Category Analysis drafters, this one never uses web search: every fact and figure
 * on the page comes from the guidance brief. The model's only job is to fit that brief into the
 * picked modules' fixed structure and character budgets, tightening wording without inventing
 * claims.
 */
public interface OnePagerDraftService {

	/**
	 * Reports whether the AI drafting engine is configured.
	 *
	 * @return true when an OpenAI API key is set
	 */
	boolean isConnected();

	/**
	 * Drafts a complete one-pager from a guidance brief.
	 *
	 * @param brief the brief and the modules to draft, in page order
	 * @return drafted page content, for user review before page creation
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the AI engine is
	 * not configured, the call fails, or the reply is incomplete
	 */
	OnePagerDraft draft(OnePagerBrief brief);
}
