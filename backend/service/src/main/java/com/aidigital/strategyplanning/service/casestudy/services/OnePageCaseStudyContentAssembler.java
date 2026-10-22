package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudySummary;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconChoice;

import java.util.List;
import java.util.Map;

/**
 * Turns reviewed One-Page Case Study content into what the deck filler and the saved record
 * need: a value for every template token, a public image URL for every chosen icon, and the
 * record-level summary derived from the tokens.
 */
public interface OnePageCaseStudyContentAssembler {

	/**
	 * Builds the value for every One-Page template token from the reviewed fields, applying each
	 * field's casing and character budget. Fields left out are written as blank so no raw token
	 * remains on the slide.
	 *
	 * @param fields reviewed values keyed by token key, may be null
	 * @return a value for every template token, in slide order
	 */
	Map<String, String> buildTokenValues(List<OnePageFieldValue> fields);

	/**
	 * Maps the chosen library icons to the public URLs Google fetches them from, skipping slots
	 * that keep the template icon and names that are not in the library. The configured public
	 * asset base URL wins over the request's own base URL when it is set, so icons can be swapped
	 * while the app runs on an address Google cannot reach.
	 *
	 * @param icons          chosen icon names keyed by slot, may be null
	 * @param requestBaseUrl base URL the current request arrived on (scheme, host, context path)
	 * @return icon image URLs keyed by slot
	 */
	Map<String, String> buildIconUrls(List<OnePageIconChoice> icons, String requestBaseUrl);

	/**
	 * Derives the record-level summary from the filled template tokens.
	 *
	 * @param tokenValues a value for every template token
	 * @return the summary persisted beside the deck link
	 */
	OnePageCaseStudySummary summarize(Map<String, String> tokenValues);
}
