package com.aidigital.strategyplanning.service.casestudy.models;

import java.util.List;

/**
 * AI-drafted One-Page Case Study content, for review before the deck is created.
 *
 * @param fields          every template text field, in slide order
 * @param icons           every icon slot with its proposed library icon
 * @param iconLibrary     every library icon the reviewer can pick from
 * @param sourceFileNames comma-separated names of the uploaded files the draft came from
 */
public record OnePageCaseStudyDraft(List<OnePageCaseStudyField> fields, List<OnePageCaseStudyIcon> icons,
									List<OnePageIconOption> iconLibrary, String sourceFileNames) {

}
