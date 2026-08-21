package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * One hyperlink to apply inside a sources line of the generated deck.
 *
 * @param text exact substring of the sources line naming the cited organization
 * @param url  official public website URL of that organization
 */
public record SourceLink(
		String text,
		String url
) {

}
