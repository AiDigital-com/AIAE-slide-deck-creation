package com.aidigital.strategyplanning.service.categoryanalysis.models;

import java.util.List;

/**
 * Command carrying reviewed field values for Standard deck creation.
 *
 * @param category   market category or industry being analysed (required)
 * @param clientName client the analysis is prepared for (required)
 * @param fields     final reviewed values for every template field (required)
 * @param createdBy  Clerk user ID of the creator (required)
 */
public record CreateStandardDeckCommand(
		String category,
		String clientName,
		List<StandardFieldValue> fields,
		String createdBy
) {

}
