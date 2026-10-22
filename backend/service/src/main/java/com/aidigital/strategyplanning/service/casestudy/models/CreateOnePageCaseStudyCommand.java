package com.aidigital.strategyplanning.service.casestudy.models;

import java.util.List;

/**
 * Reviewed One-Page Case Study content to build the deck from.
 *
 * @param fields          reviewed text values keyed by token key; blank clears the box
 * @param icons           chosen library icons keyed by icon slot; null keeps the template icon
 * @param sourceFileNames comma-separated names of the source files, may be null
 * @param createdBy       Clerk user ID of the creator
 */
public record CreateOnePageCaseStudyCommand(List<OnePageFieldValue> fields, List<OnePageIconChoice> icons,
											String sourceFileNames, String createdBy) {

}
