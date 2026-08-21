package com.aidigital.strategyplanning.service.rndrequest.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable service-layer record for a triaged RnD request.
 * Returned by all RnD request service methods; never exposes JPA entity objects.
 *
 * @param id                unique identifier
 * @param title             short title of the requested capability
 * @param requesterTeam     growth team or requester name, may be null
 * @param requestDetails    full request description, may be null
 * @param capabilityNotes   manual capability/workaround notes, may be null
 * @param buyAmount         deal size in USD used for triage
 * @param decision          triage decision code (ESCALATE_TO_RND or WORKAROUND)
 * @param capabilitySummary capability search result summary, may be null
 * @param responseDraft     drafted relay-ready response or RnD summary, may be null
 * @param status            generation status code
 * @param createdBy         Clerk user ID of the creator
 * @param createdAt         UTC timestamp of creation
 */
public record RndRequestRecord(
		Long id,
		String title,
		String requesterTeam,
		String requestDetails,
		String capabilityNotes,
		BigDecimal buyAmount,
		String decision,
		String capabilitySummary,
		String responseDraft,
		String status,
		String createdBy,
		LocalDateTime createdAt
) {

}
