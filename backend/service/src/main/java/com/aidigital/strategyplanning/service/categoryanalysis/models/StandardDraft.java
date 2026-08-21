package com.aidigital.strategyplanning.service.categoryanalysis.models;

import java.util.List;

/**
 * Complete AI-drafted field set for the Standard Category Analysis template.
 *
 * @param fields    all drafted template fields ordered by slide
 * @param alignment website-based confirmation the deck matches the client's business focus;
 *                  null for single-slide redrafts where no website review is performed
 */
public record StandardDraft(
		List<StandardDraftField> fields,
		DraftAlignment alignment
) {

}
