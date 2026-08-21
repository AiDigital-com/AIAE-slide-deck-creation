package com.aidigital.strategyplanning.service.rndrequest.models;

import java.math.BigDecimal;

/**
 * Command object carrying all fields required to create and triage a new RnD request.
 *
 * @param title           short title of the requested capability (required)
 * @param requesterTeam   growth team or requester name, may be null
 * @param requestDetails  full request description, may be null
 * @param capabilityNotes manual capability/workaround notes, may be null
 * @param buyAmount       deal size in USD used for triage (required)
 * @param createdBy       Clerk user ID of the creator (required)
 */
public record CreateRndRequestCommand(
		String title,
		String requesterTeam,
		String requestDetails,
		String capabilityNotes,
		BigDecimal buyAmount,
		String createdBy
) {

}
