package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;

import java.util.List;
import java.util.Map;

/**
 * Assembles the chat completions request bodies for the One-Page Case Study.
 *
 * <p>The prompts are product behaviour — they decide what the model drafts, how much it writes
 * into each box, and in what shape it replies — so they live in one place rather than inline in
 * the calling service.
 */
public interface OnePageDraftPromptComposer {

	/**
	 * Builds the first drafting request: the full prompt with every document's text, followed by
	 * every image as an inline data URL.
	 *
	 * @param documents parsed text documents
	 * @param images    uploaded images
	 * @return serialized JSON request body
	 */
	String buildDraftRequestBody(List<SourceDocument> documents, List<SourceImage> images);

	/**
	 * Builds a revision request: each misfit's current text, how many lines it fills now, its box
	 * size, and roughly how many characters to add or cut — plus the source material again.
	 *
	 * @param misfits   fields to rewrite
	 * @param values    current values keyed by field
	 * @param documents parsed text documents
	 * @param images    uploaded images
	 * @return serialized JSON request body
	 */
	String buildRevisionRequestBody(List<OnePageFieldSpec> misfits, Map<String, String> values,
			List<SourceDocument> documents, List<SourceImage> images);

	/**
	 * Describes a field's box for the model, e.g. "5-6 lines, about 270-320 characters".
	 *
	 * @param spec field
	 * @return box size text
	 */
	String boxSize(OnePageFieldSpec spec);
}
