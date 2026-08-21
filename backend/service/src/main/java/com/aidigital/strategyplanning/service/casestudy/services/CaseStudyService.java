package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;

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
