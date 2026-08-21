package com.aidigital.strategyplanning.service.rfpoutline.models;

/**
 * Command object carrying all fields required to create a new RFP outline.
 *
 * @param title              RFP outline title (required)
 * @param clientName         client name, may be null
 * @param industry           client industry, may be null
 * @param challenge          the client's core challenge, may be null
 * @param opportunity        the differentiator/angle for this client, may be null
 * @param solution           the proposed strategic approach, may be null
 * @param outcome            what success looks like, may be null
 * @param deckOutline        slide-by-slide response deck outline, may be null
 * @param supplementaryNotes free-text context supplied alongside the uploaded RFP, may be null
 * @param sourceFileName     uploaded source file name(s), may be null
 * @param createdBy          Clerk user ID of the creator (required)
 */
public record CreateRfpOutlineCommand(
		String title,
		String clientName,
		String industry,
		String challenge,
		String opportunity,
		String solution,
		String outcome,
		String deckOutline,
		String supplementaryNotes,
		String sourceFileName,
		String createdBy
) {

}
