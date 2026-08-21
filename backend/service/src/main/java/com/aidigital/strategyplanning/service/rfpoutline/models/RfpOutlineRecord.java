package com.aidigital.strategyplanning.service.rfpoutline.models;

import java.time.LocalDateTime;

/**
 * Immutable service-layer record for an RFP outline.
 * Returned by all RFP outline service methods; never exposes JPA entity objects.
 *
 * @param id                 unique identifier
 * @param title              RFP outline title
 * @param clientName         client name, may be null
 * @param industry           client industry, may be null
 * @param challenge          the client's core challenge, may be null
 * @param opportunity        the differentiator/angle for this client, may be null
 * @param solution           the proposed strategic approach, may be null
 * @param outcome            what success looks like, may be null
 * @param deckOutline        slide-by-slide response deck outline, may be null
 * @param supplementaryNotes free-text context supplied alongside the uploaded RFP, may be null
 * @param sourceFileName     uploaded source file name(s), may be null
 * @param docUrl             URL of the generated Google Doc, may be null
 * @param status             generation status code
 * @param createdBy          Clerk user ID of the creator
 * @param createdAt          UTC timestamp of creation
 */
public record RfpOutlineRecord(
		Long id,
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
		String docUrl,
		String status,
		String createdBy,
		LocalDateTime createdAt
) {

}
