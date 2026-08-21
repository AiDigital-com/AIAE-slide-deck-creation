package com.aidigital.strategyplanning.service.casestudy.models;

import java.time.LocalDateTime;

/**
 * Immutable service-layer record for a case study.
 * Returned by all case study service methods; never exposes JPA entity objects.
 *
 * @param id             unique identifier
 * @param title          case study title
 * @param clientName     client name, may be null
 * @param industry       client industry, may be null
 * @param challenge      challenge description, may be null
 * @param solution       solution description, may be null
 * @param results        results description, may be null
 * @param keyMetrics     key metrics, may be null
 * @param timeline       project timeline, may be null
 * @param testimonial    client testimonial, may be null
 * @param sourceFileName uploaded source file name, may be null
 * @param templateUrl    Google Slides template URL, may be null
 * @param slidesUrl      URL of the generated Google Slides deck, may be null
 * @param status         generation status code
 * @param createdBy      Clerk user ID of the creator
 * @param createdAt      UTC timestamp of creation
 */
public record CaseStudyRecord(
		Long id,
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
		String templateUrl,
		String slidesUrl,
		String status,
		String createdBy,
		LocalDateTime createdAt
) {

}
