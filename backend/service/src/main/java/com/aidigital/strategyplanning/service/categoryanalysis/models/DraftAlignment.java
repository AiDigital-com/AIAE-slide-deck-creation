package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * Website-based confirmation that a drafted deck matches the client's business focus.
 *
 * @param clientBusinessFocus short summary of the client's core business, derived from their website
 * @param confirmed           whether the website could be read to actually check alignment; when
 *                            false the match result is unverified
 * @param matches             whether the analysed category clearly fits the client's business focus,
 *                            meaningful only when confirmed is true
 * @param message             human-readable confirmation or warning about topic/business alignment
 */
public record DraftAlignment(
		String clientBusinessFocus,
		boolean confirmed,
		boolean matches,
		String message
) {

}
