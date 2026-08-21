package com.aidigital.strategyplanning.service.rndrequest.services;

import com.aidigital.strategyplanning.service.rndrequest.models.CreateRndRequestCommand;
import com.aidigital.strategyplanning.service.rndrequest.models.RndRequestRecord;

import java.util.List;

/**
 * Service contract for the RnD Request Triage aggregate.
 * Triage rule: a buy at or above the configured escalation threshold is a valid
 * RnD escalation; anything below gets a positive workaround response draft.
 */
public interface RndRequestService {

	/**
	 * Creates a new RnD request, triages it by buy amount, runs the capability
	 * scan and composes the drafted response before persisting.
	 *
	 * @param command all fields required to create the RnD request
	 * @return the persisted, triaged RnD request record
	 */
	RndRequestRecord create(CreateRndRequestCommand command);

	/**
	 * Returns all RnD requests belonging to the specified user, ordered by creation date descending.
	 *
	 * @param userId Clerk user ID of the owner
	 * @return list of RnD request records for that user
	 */
	List<RndRequestRecord> listByUser(String userId);

	/**
	 * Returns a single RnD request by ID, enforcing ownership by the requesting user.
	 *
	 * @param id     RnD request identifier
	 * @param userId Clerk user ID of the owner
	 * @return the matching RnD request record
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with NOT_FOUND reason when the request
	 * does not exist or does not belong to the user
	 */
	RndRequestRecord getById(Long id, String userId);
}
