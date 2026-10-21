package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;

import java.util.List;

/**
 * Service contract for AI-drafting the One-Page Case Study from uploaded source material.
 */
public interface OnePageCaseStudyDraftService {

	/**
	 * Drafts every template field and proposes an icon for every icon slot. Facts come only from
	 * the uploads; titles and labels may be AI-phrased. Unsupported fields come back empty. Every
	 * value is then measured in its real box and revised until it fits.
	 *
	 * @param documents parsed text documents
	 * @param images    uploaded screenshots/images, read directly by the model
	 * @return drafted content for review
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the drafting
	 * engine is not connected or the call fails
	 */
	OnePageCaseStudyDraft draft(List<SourceDocument> documents, List<SourceImage> images);

	/**
	 * Measures values against their boxes on the slide (lines, fill target, line limit, fit), so
	 * the review form can show whether an edited value still fills its box without overflowing.
	 *
	 * @param values reviewed values keyed by field; fields left out are measured as blank
	 * @return every field in slide order, with its fit
	 */
	List<OnePageCaseStudyField> measureFields(List<OnePageFieldValue> values);
}
