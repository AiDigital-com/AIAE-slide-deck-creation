package com.aidigital.strategyplanning.service.rfpoutline.services;

import com.aidigital.strategyplanning.service.common.files.SourceDocument;

import java.util.List;

/**
 * Assembles the chat completions request body for the RFP Outline Generator.
 *
 * <p>The prompt is product behaviour: it tells the model to ignore procurement boilerplate and
 * draft an internal alignment document rather than client-facing copy. It lives in one place so
 * that intent is reviewable on its own.
 */
public interface RfpOutlinePromptComposer {

	/**
	 * Builds the request body that drafts every POV field from the source documents.
	 *
	 * @param documents parsed source documents (filename + extracted text)
	 * @param notes     optional free-text supplementary context, may be null or blank
	 * @return serialized JSON request body
	 */
	String buildRequestBody(List<SourceDocument> documents, String notes);
}
