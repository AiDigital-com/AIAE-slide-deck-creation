package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.onepager.models.CreateOnePagerCommand;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDeck;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;

/**
 * Service contract for the One Page Generator: connection status, AI drafting, and page creation.
 * The created page lives in the user's Drive; a category-analysis row with the {@code ONE_PAGER}
 * template kind records its title and link for the "recent pages" list.
 */
public interface OnePagerService {

	/**
	 * Reports connection status for the One Page Generator (AI engine + user's Google access).
	 *
	 * @param userId Clerk user ID of the requesting user
	 * @return connection status flags
	 */
	StandardConnections getConnections(String userId);

	/**
	 * AI-drafts a complete one-pager from a guidance brief, sized to each picked module's fixed
	 * shape. The brief is the only source of facts; nothing is researched or invented.
	 *
	 * @param brief the brief and the modules to draft, in page order
	 * @return drafted page content, for user review before page creation
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the AI engine is
	 * not configured or drafting fails
	 */
	OnePagerDraft draft(OnePagerBrief brief);

	/**
	 * Assembles the one-pager from the master template, saves it as a new Google Slides file in
	 * the user's Drive, then records it as a category-analysis row.
	 *
	 * @param command reviewed content, shell choice, and creator
	 * @return the recorded row's identifier with the page's title and Slides URL
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when Google access is
	 * missing, a Google call fails, or the page cannot be assembled
	 */
	OnePagerDeck createDeck(CreateOnePagerCommand command);
}
