package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;

import java.util.List;

/**
 * Assembles the chat completions request bodies for the Case Study Builder.
 *
 * <p>The prompts are product behaviour — they decide what the model drafts and in what shape it
 * replies — so they live in one place rather than inline in the calling service.
 */
public interface CaseStudyPromptComposer {

	/**
	 * Builds the request body that extracts every case study field from the source documents and
	 * screenshots. Documents are inlined as text; images are attached to the same user message as
	 * inline data URLs so the model reads figures straight off them.
	 *
	 * @param documents parsed source documents (filename + extracted text), may be empty
	 * @param images    uploaded images, may be empty
	 * @return serialized JSON request body
	 */
	String buildRequestBody(List<SourceDocument> documents, List<SourceImage> images);

	/**
	 * Builds the request body that rewrites reviewed fields into the template's token structure.
	 *
	 * @param command reviewed case study fields
	 * @return serialized JSON request body
	 */
	String buildTokenMappingRequestBody(CreateCaseStudyCommand command);
}
