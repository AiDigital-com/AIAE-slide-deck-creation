package com.aidigital.strategyplanning.service.rndrequest.services.impl;

import com.aidigital.strategyplanning.service.rndrequest.models.CreateRndRequestCommand;
import com.aidigital.strategyplanning.service.rndrequest.services.RndResponseDraftComposer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Default implementation of {@link RndResponseDraftComposer}.
 * Produces deterministic, template-based drafts the triager can relay as-is.
 */
@Service
public class RndResponseDraftComposerImpl implements RndResponseDraftComposer {

	@Override
	public String composeEscalation(CreateRndRequestCommand command, BigDecimal threshold) {
		String team = StringUtils.hasText(command.requesterTeam()) ? command.requesterTeam() : "Growth team";
		String details = StringUtils.hasText(command.requestDetails())
				? command.requestDetails()
				: "See request title.";
		return "RND ESCALATION — DRAFT SUBMISSION\n"
				+ "\n"
				+ "Request: " + command.title() + "\n"
				+ "Requested by: " + team + "\n"
				+ "Deal size (the buy): " + formatUsd(command.buyAmount())
				+ " — meets the " + formatUsd(threshold) + " escalation threshold.\n"
				+ "\n"
				+ "Summary for RnD:\n"
				+ details + "\n"
				+ "\n"
				+ "Recommendation: valid RnD escalation. Please review scope, feasibility and timeline, "
				+ "and confirm an owner so we can update the requesting team.";
	}

	@Override
	public String composeWorkaround(CreateRndRequestCommand command) {
		String team = StringUtils.hasText(command.requesterTeam()) ? command.requesterTeam() : "team";
		String capabilityLine = StringUtils.hasText(command.capabilityNotes())
				? "Good news — here's what we can already do today that gets close: "
				+ command.capabilityNotes() + "\n\n"
				: "Good news — there may already be existing capabilities that get close to this; "
				+ "we're checking our capability library and will share the closest match.\n\n";
		return "RELAY-READY RESPONSE (WORKAROUND)\n"
				+ "\n"
				+ "Hi " + team + ",\n"
				+ "\n"
				+ "Thanks for raising \"" + command.title() + "\" — great to see this demand signal from the deal "
				+ "(" + formatUsd(command.buyAmount()) + ").\n"
				+ "\n"
				+ capabilityLine
				+ "Suggested next step: let's set up a quick session to map the client's goal onto what's live today "
				+ "so you can keep the deal moving now. In parallel, we've logged this request so it feeds directly "
				+ "into the RnD roadmap review — if the opportunity grows, it can be fast-tracked for escalation.\n"
				+ "\n"
				+ "Keep these coming — this is exactly the input that shapes what we build next.";
	}

	/**
	 * Formats an amount as a whole-dollar USD string (e.g. $100,000).
	 *
	 * @param amount the amount to format
	 * @return formatted USD string
	 */
	String formatUsd(BigDecimal amount) {
		NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
		format.setMaximumFractionDigits(0);
		return format.format(amount);
	}
}
