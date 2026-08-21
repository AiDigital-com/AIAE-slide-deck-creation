package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * Command object carrying all fields required to create a new category analysis.
 *
 * @param title           analysis title (required)
 * @param category        market category, may be null
 * @param marketOverview  market overview, may be null
 * @param keyPlayers      key players, may be null
 * @param trends          market trends, may be null
 * @param opportunities   strategic opportunities, may be null
 * @param recommendations recommendations, may be null
 * @param sourceFileNames comma-separated source file names, may be null
 * @param createdBy       Clerk user ID of the creator (required)
 */
public record CreateCategoryAnalysisCommand(
		String title,
		String category,
		String marketOverview,
		String keyPlayers,
		String trends,
		String opportunities,
		String recommendations,
		String sourceFileNames,
		String createdBy
) {

}
