package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;

import java.util.List;
import java.util.Map;

/**
 * Assembles the OpenAI request bodies for the In-Depth Category Analysis deck.
 *
 * <p>Drafting is several calls, not one: a knowledge-only draft of every checked field in strict
 * JSON mode (chat completions), a {@code web_search}-grounded fact-check per topic group, a
 * business-focus alignment check, and a {@code web_search}-grounded single-slide redraft
 * (Responses API). Each prompt enumerates the exact template tokens the model must fill, so they
 * are product behaviour and live in one reviewable place.
 */
public interface InDepthDraftPromptComposer {

	/**
	 * Builds the chat completions body that drafts every checked field from the model's own
	 * knowledge, in strict JSON mode.
	 *
	 * @param category      market category being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes optional steering notes, may be null
	 * @param clientWebsite client website URL, included for wording context
	 * @param websiteText   readable website text for wording context, or null when unavailable
	 * @param tokens        every distinct token to draft
	 * @return serialized JSON request body
	 */
	String buildAllFieldsRequestBody(String category, String clientName, String guidanceNotes,
			String clientWebsite, String websiteText, List<InDepthTokenSpec> tokens);

	/**
	 * Builds the Responses API body that fact-checks one topic group's drafted statistics with
	 * the {@code web_search} tool.
	 *
	 * @param category     market category being analysed
	 * @param group        the topic's factual token specs to verify
	 * @param draftedByKey drafted values, keyed by token key
	 * @return serialized JSON request body
	 */
	String buildVerifyRequestBody(String category, List<InDepthTokenSpec> group,
			Map<String, InDepthDraftField> draftedByKey);

	/**
	 * Builds the Responses API body for the business-focus alignment check, in JSON mode.
	 *
	 * @param category      market category being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param clientWebsite client website URL under review
	 * @param websiteText   readable website text used for alignment, or null when unavailable
	 * @return serialized JSON request body
	 */
	String buildAlignmentRequestBody(String category, String clientName, String clientWebsite,
			String websiteText);

	/**
	 * Builds the Responses API body that redrafts one slide's fields with the {@code web_search}
	 * tool, with the current values of the checked deck supplied as context.
	 *
	 * @param category      market category being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes original brief steering notes, may be null
	 * @param slideNumber   slide being redrafted
	 * @param slideNote     optional guidance for this redraft, may be null
	 * @param slideTokens   token specs belonging to the redrafted slide
	 * @param currentFields current values for every field of the checked deck, may be null
	 * @return serialized JSON request body
	 */
	String buildSlideRequestBody(String category, String clientName, String guidanceNotes,
			int slideNumber, String slideNote, List<InDepthTokenSpec> slideTokens,
			List<InDepthFieldValue> currentFields);
}
