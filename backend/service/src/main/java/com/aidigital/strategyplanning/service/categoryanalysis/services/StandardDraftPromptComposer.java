package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;

import java.util.List;

/**
 * Assembles the chat completions request bodies for the Standard Category Analysis deck.
 *
 * <p>Both the full-deck prompt and the single-slide redraft prompt enumerate the exact template
 * tokens the model must fill, so they are product behaviour and live in one reviewable place.
 */
public interface StandardDraftPromptComposer {

	/**
	 * Builds the request body that drafts the whole five-slide deck.
	 *
	 * @param category      market category being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes optional focus areas, may be null or blank
	 * @param clientWebsite client website URL as supplied in the brief
	 * @param storyTheme    optional narrative theme, may be null or blank
	 * @param websiteText   readable text of the client site, or null when unavailable
	 * @return serialized JSON request body
	 */
	String buildRequestBody(String category, String clientName, String guidanceNotes,
			String clientWebsite, String storyTheme, String websiteText);

	/**
	 * Builds the request body that redrafts one slide while keeping the rest of the deck.
	 *
	 * @param category      market category being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes original brief focus areas, may be null
	 * @param slideNumber   slide to redraft
	 * @param slideNote     optional guidance for this redraft, may be null or blank
	 * @param currentFields current values of every template field, supplied as context
	 * @return serialized JSON request body
	 */
	String buildSlideRequestBody(String category, String clientName, String guidanceNotes,
			int slideNumber, String slideNote, List<StandardFieldValue> currentFields);

	/**
	 * Returns the template tokens that belong to one slide.
	 *
	 * @param slideNumber slide number
	 * @return token specs of that slide
	 */
	List<StandardTemplateTokens.TokenSpec> slideTokens(int slideNumber);
}
