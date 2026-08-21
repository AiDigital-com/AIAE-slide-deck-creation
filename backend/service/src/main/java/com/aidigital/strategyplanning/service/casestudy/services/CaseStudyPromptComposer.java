package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;

import java.util.List;

/**
 * Assembles the chat completions request bodies for the Case Study Builder.
 *
 * <p>The prompts are product behaviour — they decide what the model drafts and in what shape it
 * replies — so they live in one place rather than inline in the calling service.
 */
public interface CaseStudyPromptComposer {

	/**
	 * Builds the request body that extracts every case study field from the source documents.
	 *
	 * @param documents parsed source documents (filename + extracted text)
	 * @return serialized JSON request body
	 */
	String buildRequestBody(List<SourceDocument> documents);

	/**
	 * Builds the request body that rewrites reviewed fields into the template's token structure.
	 *
	 * @param command reviewed case study fields
	 * @return serialized JSON request body
	 */
	String buildTokenMappingRequestBody(CreateCaseStudyCommand command);
}
