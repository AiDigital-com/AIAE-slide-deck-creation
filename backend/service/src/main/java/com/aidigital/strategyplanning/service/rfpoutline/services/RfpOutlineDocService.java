package com.aidigital.strategyplanning.service.rfpoutline.services;

import java.util.Map;

/**
 * Service contract for generating a Google Doc RFP outline in the user's own Drive.
 */
public interface RfpOutlineDocService {

	/**
	 * Creates a blank Google Doc in the user's Drive and writes the outline into it as headed sections.
	 *
	 * @param userId        Clerk user ID whose Google OAuth token is used
	 * @param docTitle      file name of the new doc, also written as its title heading
	 * @param sectionValues section text keyed by
	 *                      {@link com.aidigital.strategyplanning.service.rfpoutline.enums.RfpOutlineSection#getKey()}
	 * @return URL of the generated Google Doc
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the user is not
	 *                                                                          connected to Google with Drive and Docs
	 *                                                                          access, or when a Google API call fails
	 */
	String createDoc(String userId, String docTitle, Map<String, String> sectionValues);
}
