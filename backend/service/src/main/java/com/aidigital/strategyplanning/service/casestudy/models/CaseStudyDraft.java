package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * AI-drafted case study form fields produced from uploaded source documents.
 * Every field may be null when the documents do not support a value.
 *
 * @param title           drafted case study title
 * @param clientName      drafted client name
 * @param industry        drafted industry or sector
 * @param challenge       drafted challenge or problem statement
 * @param solution        drafted solution or approach delivered
 * @param results         drafted outcomes and results achieved
 * @param keyMetrics      drafted key quantitative metrics
 * @param timeline        drafted project timeline or duration
 * @param testimonial     drafted client quote or testimonial
 * @param sourceFileNames comma-separated names of the source documents used
 */
public record CaseStudyDraft(
		String title,
		String clientName,
		String industry,
		String challenge,
		String solution,
		String results,
		String keyMetrics,
		String timeline,
		String testimonial,
		String sourceFileNames) {

}
