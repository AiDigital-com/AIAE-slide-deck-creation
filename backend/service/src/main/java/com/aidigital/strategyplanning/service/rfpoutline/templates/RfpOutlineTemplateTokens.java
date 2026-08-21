package com.aidigital.strategyplanning.service.rfpoutline.templates;

import java.util.List;

/**
 * Canonical token registry for the RFP outline Google Doc template.
 * The doc filler replaces the {@code {{token}}} form of each key with its reviewed value.
 */
public final class RfpOutlineTemplateTokens {

	/**
	 * All token keys of the RFP outline Google Doc template.
	 */
	public static final List<String> TOKEN_KEYS = List.of(
			"client_name",
			"industry",
			"challenge",
			"opportunity",
			"solution",
			"outcome",
			"deck_outline");

	private RfpOutlineTemplateTokens() {
	}
}
