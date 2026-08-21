package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * Command object carrying all fields required to create a new case study.
 *
 * @param title          case study title (required)
 * @param clientName     client name, may be null
 * @param industry       client industry, may be null
 * @param challenge      challenge description, may be null
 * @param solution       solution description, may be null
 * @param results        results description, may be null
 * @param keyMetrics     key metrics, may be null
 * @param timeline       project timeline, may be null
 * @param testimonial    client testimonial, may be null
 * @param sourceFileName uploaded source file name, may be null
 * @param createdBy      Clerk user ID of the creator (required)
 */
public record CreateCaseStudyCommand(
		String title,
		String clientName,
		String industry,
		String challenge,
		String solution,
		String results,
		String keyMetrics,
		String timeline,
		String testimonial,
		String sourceFileName,
		String createdBy
) {

}
