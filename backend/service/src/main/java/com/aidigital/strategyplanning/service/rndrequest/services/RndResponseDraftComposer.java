package com.aidigital.strategyplanning.service.rndrequest.services;

import com.aidigital.strategyplanning.service.rndrequest.models.CreateRndRequestCommand;

import java.math.BigDecimal;

/**
 * Service contract for composing the drafted output of a triaged RnD request:
 * an RnD submission summary when escalated, or a positive relay-ready
 * workaround response otherwise (never a flat "no").
 */
public interface RndResponseDraftComposer {

	/**
	 * Composes a drafted RnD submission summary for an escalated request.
	 *
	 * @param command   the request being triaged
	 * @param threshold escalation threshold in USD that the buy met or exceeded
	 * @return drafted RnD submission summary, never null
	 */
	String composeEscalation(CreateRndRequestCommand command, BigDecimal threshold);

	/**
	 * Composes a positive, relay-ready workaround response for a request below
	 * the escalation threshold. The tone stays constructive and never says "no".
	 *
	 * @param command the request being triaged
	 * @return drafted workaround relay response, never null
	 */
	String composeWorkaround(CreateRndRequestCommand command);
}
