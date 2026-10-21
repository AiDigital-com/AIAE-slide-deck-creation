package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;

/**
 * Service contract for creating One Page Generator pages in the user's Google Slides.
 */
public interface OnePagerDeckService {

	/**
	 * Exports the master template as PPTX (read-only; the master is never copied or edited),
	 * assembles the chosen shell plus the reviewed modules into one slide, and uploads the result
	 * as a new native Google Slides file in the user's Drive.
	 *
	 * @param userId    Clerk user ID whose Google account owns the new file
	 * @param deckTitle file name for the new presentation
	 * @param shell     header/footer shell to build onto
	 * @param content   reviewed page content
	 * @return URL of the newly created Google Slides presentation
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when Google access is
	 * missing, a Google API call fails, or the page cannot be assembled
	 */
	String createDeck(String userId, String deckTitle, OnePagerShell shell, OnePagerDraft content);
}
