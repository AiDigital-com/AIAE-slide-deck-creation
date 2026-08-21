package com.aidigital.strategyplanning.service.rfpoutline.services;

import java.util.Map;

/**
 * Service contract for generating a Google Doc RFP outline in the user's own Drive.
 */
public interface RfpOutlineDocService {

	/**
	 * Copies the tokenized master template into the user's Drive and fills every template token.
	 *
	 * @param userId      Clerk user ID whose Google OAuth token is used
	 * @param docTitle    file name for the copied doc
	 * @param tokenValues template token values keyed by token key (without braces)
	 * @return URL of the generated Google Doc
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the user is not
	 * connected to Google with Drive and Docs
	 *                                                                          access, or when a Google API call fails
	 */
	String createDoc(String userId, String docTitle, Map<String, String> tokenValues);
}
