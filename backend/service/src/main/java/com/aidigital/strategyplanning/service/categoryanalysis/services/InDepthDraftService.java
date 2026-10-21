package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthFieldValue;

import java.util.List;

/**
 * Service contract for AI-drafting In-Depth Category Analysis content. Unlike the Standard
 * template's drafting service, this one grounds statistics and sources in live web search (via
 * the OpenAI Responses API's built-in {@code web_search} tool) and only drafts the slides the
 * user checked in the "check to include" preview.
 */
public interface InDepthDraftService {

	/**
	 * Reports whether the AI drafting engine is configured with an API key. Shares the same
	 * OpenAI key as the Standard template's drafting service.
	 *
	 * @return true when an OpenAI API key is configured
	 */
	boolean isConnected();

	/**
	 * Drafts every field belonging to the checked slides, fact-checks the quantitative ones
	 * against live web search, and reviews the client's website to confirm the category fits
	 * the client's actual business focus.
	 *
	 * @param category       market category or industry being analysed
	 * @param clientName     client the analysis is prepared for
	 * @param guidanceNotes  optional focus areas or constraints, may be null
	 * @param clientWebsite  client's website URL, reviewed for business-focus alignment when reachable
	 * @param selectedSlides slide numbers checked in the "check to include" preview
	 * @return drafted field set scoped to the checked slides, with alignment confirmation for
	 * user review; empty with no alignment when the checked slides carry no draftable content
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the AI engine is
	 * not configured, no slides are selected, or the drafting call fails
	 */
	InDepthDraft draftInDepth(String category, String clientName, String guidanceNotes,
	                          String clientWebsite, List<Integer> selectedSlides);

	/**
	 * Redrafts only the fields of a single slide, using the whole checked deck's current values
	 * as context and grounding the new content in live web search results.
	 *
	 * @param category      market category or industry being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes original brief focus areas or constraints, may be null
	 * @param slideNumber   slide to redraft
	 * @param slideNote     optional guidance for this redraft, may be null
	 * @param currentFields current values for every field of the checked slides, used as context
	 * @return drafted field set for the requested slide
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the AI engine is
	 * not configured, the slide has no fields, or the drafting call fails
	 */
	InDepthDraft redraftSlide(String category, String clientName, String guidanceNotes,
	                          int slideNumber, String slideNote,
	                          List<InDepthFieldValue> currentFields);
}
