package com.aidigital.strategyplanning.service.categoryanalysis.models;

import java.util.List;

/**
 * AI-drafted field set for the In-Depth Category Analysis template, scoped to whichever slides
 * were checked in the "check to include" preview.
 *
 * @param fields    drafted fields for the checked slides, ordered by the template's token registry
 * @param alignment website-based confirmation the deck matches the client's business focus;
 *                  null for single-slide redrafts and content-free selections, where no website
 *                  review is performed
 */
public record InDepthDraft(
		List<InDepthDraftField> fields,
		DraftAlignment alignment
) {

}
