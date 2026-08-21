package com.aidigital.strategyplanning.service.rfpoutline.services;

import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineDraft;

/**
 * Reads the model's JSON reply into an RFP outline draft.
 *
 * <p>Missing fields, explicit nulls, and whitespace-only values all mean "no value" rather than
 * an empty string in the generated document.
 */
public interface RfpOutlineDraftParser {

	/**
	 * Parses the model's JSON content into a draft.
	 *
	 * @param content         JSON object string returned by the model
	 * @param sourceFileNames comma-separated names of the source documents used
	 * @return parsed draft
	 */
	RfpOutlineDraft parseDraft(String content, String sourceFileNames);
}
