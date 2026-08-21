package com.aidigital.strategyplanning.service.categoryanalysis.models;

import java.time.LocalDateTime;

/**
 * Immutable service-layer record for a category analysis.
 * Returned by all category analysis service methods; never exposes JPA entity objects.
 *
 * @param id              unique identifier
 * @param title           analysis title
 * @param category        market category, may be null
 * @param marketOverview  market overview, may be null
 * @param keyPlayers      key players, may be null
 * @param trends          market trends, may be null
 * @param opportunities   strategic opportunities, may be null
 * @param recommendations recommendations, may be null
 * @param sourceFileNames comma-separated source file names, may be null
 * @param slidesUrl       URL of the generated Google Slides deck, may be null
 * @param templateKind    template used for generation (e.g. STANDARD), may be null
 * @param status          generation status code
 * @param createdBy       Clerk user ID of the creator
 * @param createdAt       UTC timestamp of creation
 */
public record CategoryAnalysisRecord(
		Long id,
		String title,
		String category,
		String marketOverview,
		String keyPlayers,
		String trends,
		String opportunities,
		String recommendations,
		String sourceFileNames,
		String slidesUrl,
		String templateKind,
		String status,
		String createdBy,
		LocalDateTime createdAt
) {

}
