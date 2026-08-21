package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;

import java.util.List;

/**
 * Service contract for AI-drafting Standard Category Analysis content.
 */
public interface StandardDraftService {

	/**
	 * Reports whether the AI drafting engine is configured with an API key.
	 *
	 * @return true when an OpenAI API key is configured
	 */
	boolean isConnected();

	/**
	 * Drafts every text field of the Standard Category Analysis template
	 * from a short brief. Reviews the client's website to tailor the content and confirm the
	 * deck matches the client's business focus.
	 *
	 * @param category      market category or industry being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes optional focus areas or constraints, may be null
	 * @param clientWebsite client's website URL, reviewed for business-focus alignment
	 * @param storyTheme    optional narrative theme guiding the deck's arc, may be null
	 * @return complete drafted field set with alignment confirmation for user review
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the AI engine is
	 * not configured or the drafting call fails
	 */
	StandardDraft draftStandard(String category, String clientName, String guidanceNotes,
	                            String clientWebsite, String storyTheme);

	/**
	 * Redrafts only the fields of a single slide of the
	 * Standard Category Analysis template. The current values of the whole deck are supplied as
	 * context so the redraft stays consistent with the slides the user is keeping.
	 *
	 * @param category      market category or industry being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes original brief focus areas or constraints, may be null
	 * @param slideNumber   slide to redraft (1-5)
	 * @param slideNote     optional guidance for this redraft, may be null
	 * @param currentFields current values for every template field, used as context
	 * @return drafted field set for the requested slide
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the AI engine is
	 * not configured or the drafting call fails
	 */
	StandardDraft redraftSlide(String category, String clientName, String guidanceNotes,
	                           int slideNumber, String slideNote,
	                           List<StandardFieldValue> currentFields);
}
