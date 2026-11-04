package com.aidigital.strategyplanning.service.rfpoutline.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The sections of a generated RFP outline document, in the order they are written. Each pairs
 * the key the drafting service fills with the heading the Google Doc shows above its text.
 */
@Getter
@RequiredArgsConstructor
public enum RfpOutlineSection {

	CLIENT_NAME("client_name", "Client"),
	INDUSTRY("industry", "Industry"),
	CHALLENGE("challenge", "Challenge"),
	OPPORTUNITY("opportunity", "Opportunity"),
	SOLUTION("solution", "Solution"),
	OUTCOME("outcome", "Outcome"),
	DECK_OUTLINE("deck_outline", "Response Deck Outline");

	/** Key under which the section's text is passed to the doc writer. */
	private final String key;

	/** Heading written above the section's text. */
	private final String heading;
}
