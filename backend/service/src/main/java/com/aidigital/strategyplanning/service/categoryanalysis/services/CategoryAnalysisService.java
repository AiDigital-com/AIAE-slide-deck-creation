package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateCategoryAnalysisCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;

import java.util.List;

/**
 * Service contract for the Category Analysis Builder aggregate.
 * Manages creation and retrieval of category analyses for 3-5 slide Google Slides generation.
 */
public interface CategoryAnalysisService {

	/**
	 * Creates a new category analysis from the provided command and sets its status to SUBMITTED.
	 *
	 * @param command all fields required to create the category analysis
	 * @return the persisted category analysis record
	 */
	CategoryAnalysisRecord create(CreateCategoryAnalysisCommand command);

	/**
	 * Returns all category analyses belonging to the specified user, ordered by creation date descending.
	 *
	 * @param userId Clerk user ID of the owner
	 * @return list of category analysis records for that user
	 */
	List<CategoryAnalysisRecord> listByUser(String userId);

	/**
	 * Returns a single category analysis by ID, enforcing ownership by the requesting user.
	 *
	 * @param id     category analysis identifier
	 * @param userId Clerk user ID of the owner
	 * @return the matching category analysis record
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with NOT_FOUND reason when the
	 * analysis does not exist or does not belong to the user
	 */
	CategoryAnalysisRecord getById(Long id, String userId);

	/**
	 * Reports connection status for the Standard deck generator (AI engine + user's Google access).
	 *
	 * @param userId Clerk user ID of the requesting user
	 * @return connection status flags
	 */
	StandardConnections getStandardConnections(String userId);

	/**
	 * AI-drafts every field of the Standard Category Analysis template from a short brief,
	 * reviewing the client's website to tailor content and confirm business-focus alignment.
	 *
	 * @param category      market category or industry being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes optional focus areas or constraints, may be null
	 * @param clientWebsite client's website URL, reviewed for business-focus alignment
	 * @param storyTheme    optional narrative theme guiding the deck's arc, may be null
	 * @return complete drafted field set with alignment confirmation for user review
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the AI engine is
	 * not configured or drafting fails
	 */
	StandardDraft draftStandard(String category, String clientName, String guidanceNotes,
	                            String clientWebsite, String storyTheme);

	/**
	 * AI-redrafts only the fields of a single slide of the
	 * Standard Category Analysis template, using the whole deck's current values as context.
	 *
	 * @param category      market category or industry being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes original brief focus areas or constraints, may be null
	 * @param slideNumber   slide to redraft (1-5)
	 * @param slideNote     optional guidance for this redraft, may be null
	 * @param currentFields current values for every template field, used as context
	 * @return drafted field set for the requested slide
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the AI engine is
	 * not configured or drafting fails
	 */
	StandardDraft redraftSlide(String category, String clientName, String guidanceNotes,
	                           int slideNumber, String slideNote,
	                           List<StandardFieldValue> currentFields);

	/**
	 * Creates the Standard deck in the user's Google Slides from reviewed field values
	 * and persists the resulting analysis record with its Slides URL.
	 *
	 * @param command       reviewed field values and creator
	 * @param publicBaseUrl externally reachable base URL of this backend (scheme + host), used to
	 *                      host generated slide images so Google Slides can fetch them
	 * @return the persisted category analysis record including the Slides URL
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when Google access is
	 * missing or deck creation fails
	 */
	CategoryAnalysisRecord createStandardDeck(CreateStandardDeckCommand command, String publicBaseUrl);
}
