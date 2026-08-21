package com.aidigital.strategyplanning.service.rfpoutline.services;

import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineRecord;

import java.util.List;

/**
 * Service contract for the RFP Outline Generator aggregate.
 * Manages creation and retrieval of RFP outlines, including Google Doc generation.
 */
public interface RfpOutlineService {

	/**
	 * Creates a new RFP outline from the provided command and generates its Google Doc.
	 *
	 * @param command all fields required to create the RFP outline
	 * @return the persisted RFP outline record
	 */
	RfpOutlineRecord create(CreateRfpOutlineCommand command);

	/**
	 * Returns all RFP outlines belonging to the specified user, ordered by creation date descending.
	 *
	 * @param userId Clerk user ID of the owner
	 * @return list of RFP outline records for that user
	 */
	List<RfpOutlineRecord> listByUser(String userId);

	/**
	 * Returns a single RFP outline by ID, enforcing ownership by the requesting user.
	 *
	 * @param id     RFP outline identifier
	 * @param userId Clerk user ID of the owner
	 * @return the matching RFP outline record
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with NOT_FOUND reason when the RFP
	 * outline does not exist or does not belong to the user
	 */
	RfpOutlineRecord getById(Long id, String userId);
}
