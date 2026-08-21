package com.aidigital.strategyplanning.service.rfpoutline.services;

import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineDraft;

import java.util.List;
import java.util.Map;

/**
 * Service contract for AI-drafting RFP outline fields from uploaded RFP source documents.
 */
public interface RfpOutlineDraftService {

	/**
	 * Reports whether the AI drafting engine is configured.
	 *
	 * @return true when an OpenAI API key is present
	 */
	boolean isConnected();

	/**
	 * Drafts every RFP outline field from the extracted text of the uploaded RFP documents plus any
	 * supplementary free-text notes. Ignores procurement/legal/compliance boilerplate in the
	 * source text and drafts the deck outline following the house narrative arc.
	 *
	 * @param documents parsed source documents (filename + extracted text), between 1 and 10
	 * @param notes     optional free-text supplementary context, may be null or blank
	 * @return drafted RFP outline fields for user review
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the drafting
	 * engine is not connected or the call fails
	 */
	RfpOutlineDraft draft(List<SourceDocument> documents, String notes);

	/**
	 * Maps the reviewed RFP outline fields onto the Google Doc template tokens.
	 *
	 * @param command reviewed RFP outline fields
	 * @return values for every template token key, never null
	 */
	Map<String, String> buildTemplateTokenValues(CreateRfpOutlineCommand command);
}
