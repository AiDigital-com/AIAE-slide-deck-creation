package com.aidigital.strategyplanning.service.rfpoutline.models;

/**
 * AI-drafted RFP outline fields produced from uploaded RFP source documents (plus optional
 * supplementary notes). Every field may be null when the input does not support a value.
 *
 * @param title           drafted RFP outline title
 * @param clientName      drafted client name
 * @param industry        drafted client industry or vertical
 * @param challenge       drafted core challenge/need
 * @param opportunity     drafted differentiator/angle for this client
 * @param solution        drafted proposed strategic approach
 * @param outcome         drafted description of what success looks like
 * @param deckOutline     drafted slide-by-slide response deck outline
 * @param sourceFileNames comma-separated names of the source documents used
 */
public record RfpOutlineDraft(
		String title,
		String clientName,
		String industry,
		String challenge,
		String opportunity,
		String solution,
		String outcome,
		String deckOutline,
		String sourceFileNames) {

}
