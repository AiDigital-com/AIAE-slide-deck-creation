package com.aidigital.strategyplanning.service.categoryanalysis.models;

import java.util.List;

/**
 * Command carrying reviewed field values and the checked slide set for In-Depth deck creation.
 *
 * @param category       market category or industry being analysed (required)
 * @param clientName     client the analysis is prepared for (required)
 * @param selectedSlides slide numbers checked in the "check to include" preview (required)
 * @param fields         final reviewed values for every field of the checked slides (required)
 * @param createdBy      Clerk user ID of the creator (required)
 */
public record CreateInDepthDeckCommand(
		String category,
		String clientName,
		List<Integer> selectedSlides,
		List<InDepthFieldValue> fields,
		String createdBy
) {

}
