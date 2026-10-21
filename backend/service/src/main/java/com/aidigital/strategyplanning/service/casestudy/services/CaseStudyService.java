package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.models.CreateOnePageCaseStudyCommand;

import java.util.List;

/**
 * Service contract for the Case Study Builder aggregate.
 * Manages creation and retrieval of case studies for Google Slides generation.
 */
public interface CaseStudyService {

	/**
	 * Creates a new case study from the provided command and sets its status to SUBMITTED.
	 * The tokenized Google Slides template URL is automatically attached.
	 *
	 * @param command all fields required to create the case study
	 * @return the persisted case study record
	 */
	CaseStudyRecord create(CreateCaseStudyCommand command);

	/**
	 * Creates a One-Page Case Study deck from reviewed content and records it. Every template
	 * token is filled (blank values clear their box) and each chosen library icon replaces its
	 * tagged template icon; the template's hero photo stays as a placeholder for the user. The
	 * deck is published before the row is saved, so a failed publish leaves nothing behind.
	 *
	 * @param command        reviewed fields, icon choices, and creator
	 * @param requestBaseUrl base URL the current request arrived on, used to serve the icon images
	 *                       when no public asset base URL is configured
	 * @return the persisted case study record including the Slides URL
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when Google access is
	 * missing or deck creation fails
	 */
	CaseStudyRecord createOnePage(CreateOnePageCaseStudyCommand command, String requestBaseUrl);

	/**
	 * Returns all case studies belonging to the specified user, ordered by creation date descending.
	 *
	 * @param userId Clerk user ID of the owner
	 * @return list of case study records for that user
	 */
	List<CaseStudyRecord> listByUser(String userId);

	/**
	 * Returns a single case study by ID, enforcing ownership by the requesting user.
	 *
	 * @param id     case study identifier
	 * @param userId Clerk user ID of the owner
	 * @return the matching case study record
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with NOT_FOUND reason when the case
	 * study does not exist or does not belong to the user
	 */
	CaseStudyRecord getById(Long id, String userId);
}
