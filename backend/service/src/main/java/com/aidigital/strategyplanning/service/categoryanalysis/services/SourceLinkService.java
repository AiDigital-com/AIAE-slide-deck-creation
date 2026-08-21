package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.SourceLink;

import java.util.List;
import java.util.Map;

/**
 * Resolves the research bodies cited in the deck's sources lines to their official
 * public website URLs so the finished deck can hyperlink each citation.
 */
public interface SourceLinkService {

	/**
	 * Resolves hyperlinks for the given sources lines.
	 *
	 * @param sourcesByKey final reviewed sources text keyed by template token key
	 * @return per token key, the links to apply (text is an exact substring of that
	 * sources line); empty map when resolution is unavailable or fails
	 */
	Map<String, List<SourceLink>> resolveSourceLinks(Map<String, String> sourcesByKey);
}
