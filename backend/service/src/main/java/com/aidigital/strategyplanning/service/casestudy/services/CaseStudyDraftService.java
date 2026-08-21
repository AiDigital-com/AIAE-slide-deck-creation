package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;

import java.util.List;
import java.util.Map;

/**
 * Service contract for AI-drafting case study form fields from uploaded source documents.
 */
public interface CaseStudyDraftService {

	/**
	 * Reports whether the AI drafting engine is configured.
	 *
	 * @return true when an OpenAI API key is present
	 */
	boolean isConnected();

	/**
	 * Drafts every case study form field from the extracted text of the source documents.
	 *
	 * @param documents parsed source documents (filename + extracted text), between 1 and 10
	 * @return drafted case study fields for user review
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the drafting
	 * engine is not connected or the call fails
	 */
	CaseStudyDraft draft(List<SourceDocument> documents);

	/**
	 * Maps the reviewed case study form fields onto the Google Slides template tokens.
	 * Uses the AI engine when connected for a natural rewrite into the template's structure
	 * (priorities, result details, three metric value/label pairs); otherwise falls back to a
	 * deterministic mapping so deck generation still works without AI.
	 *
	 * @param command reviewed case study fields
	 * @return values for every template token key, never null
	 */
	Map<String, String> buildTemplateTokenValues(CreateCaseStudyCommand command);
}
