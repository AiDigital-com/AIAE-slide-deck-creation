package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * The record-level summary of a One-Page Case Study, derived from its filled template tokens so
 * the saved row reads like the three-page case studies beside it.
 *
 * @param title      deck title (the two title lines joined)
 * @param clientName generic client description, may be null
 * @param industry   main topic, may be null
 * @param challenge  challenge text, may be null
 * @param solution   tactics joined with commas, may be null
 * @param results    results summary, may be null
 * @param keyMetrics headline stat and stat values joined with commas, may be null
 * @param timeline   flight dates, may be null
 */
public record OnePageCaseStudySummary(String title, String clientName, String industry, String challenge,
									  String solution, String results, String keyMetrics, String timeline) {

}
