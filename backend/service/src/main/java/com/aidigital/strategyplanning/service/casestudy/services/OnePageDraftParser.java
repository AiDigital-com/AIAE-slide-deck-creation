package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyDraft;

import java.util.Map;

/**
 * Turns the model's JSON replies into One-Page Case Study drafts, normalizing every value against
 * the template and dropping placeholder non-answers.
 */
public interface OnePageDraftParser {

	/**
	 * Parses the model's first draft, normalizing every value against the template. Fields are
	 * returned unmeasured; the draft service measures them after the revision loop.
	 *
	 * @param content         JSON object string returned by the model
	 * @param sourceFileNames comma-separated names of the uploads
	 * @return the normalized draft
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the reply is
	 * not valid JSON
	 */
	OnePageCaseStudyDraft parseDraft(String content, String sourceFileNames);

	/**
	 * Parses a revision reply into the rewritten raw values keyed by field, leaving out
	 * placeholder non-answers.
	 *
	 * @param content JSON object string returned by the model
	 * @return rewritten raw values keyed by field key
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the reply is
	 * not valid JSON
	 */
	Map<String, String> parseRevisions(String content);

	/**
	 * Reports whether a model reply means "no value".
	 *
	 * @param value raw reply value
	 * @return true for blank values and placeholder non-answers
	 */
	boolean isNonAnswer(String value);
}
