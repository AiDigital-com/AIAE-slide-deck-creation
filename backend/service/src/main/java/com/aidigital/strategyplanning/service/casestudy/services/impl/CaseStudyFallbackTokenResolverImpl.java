package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyFallbackTokenResolver;
import com.aidigital.strategyplanning.service.casestudy.templates.CaseStudyTemplateTokens;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link CaseStudyFallbackTokenResolver}.
 */
@Service
public class CaseStudyFallbackTokenResolverImpl implements CaseStudyFallbackTokenResolver {

	@Override
	public Map<String, String> resolve(CreateCaseStudyCommand command) {
		Map<String, String> values = new LinkedHashMap<>();
		for (String key : CaseStudyTemplateTokens.TOKEN_KEYS) {
			values.put(key, "");
		}
		values.put("client_vertical", firstNonBlank(command.industry(), command.clientName(),
				command.title()));
		values.put("client_challenge", nullToEmpty(command.challenge()));
		values.put("solution_body", nullToEmpty(command.solution()));
		String results = nullToEmpty(command.results());
		String[] sentences = results.split("(?<=[.!?])\\s+");
		values.put("results_intro", sentences.length > 0 ? sentences[0] : "");
		for (int i = 0; i < 3 && i + 1 < sentences.length; i++) {
			values.put("result_detail_" + (i + 1), sentences[i + 1]);
		}
		String[] metrics = nullToEmpty(command.keyMetrics()).split(",");
		Pattern figure = Pattern.compile("^\\s*([$€£]?[\\d.,]+\\s*[%xX]?[A-Za-z]{0,2})\\s*(.*)$");
		for (int i = 0; i < 3 && i < metrics.length; i++) {
			String metric = metrics[i].trim();
			if (metric.isEmpty()) {
				continue;
			}
			Matcher matcher = figure.matcher(metric);
			if (matcher.matches() && StringUtils.hasText(matcher.group(2))) {
				values.put("metric_" + (i + 1) + "_value", matcher.group(1).trim());
				values.put("metric_" + (i + 1) + "_label", matcher.group(2).trim());
			} else {
				values.put("metric_" + (i + 1) + "_value", metric);
			}
		}
		return values;
	}

	/**
	 * Returns the first argument that has text, or an empty string when none do.
	 *
	 * @param candidates candidate values in priority order
	 * @return first non-blank candidate, or empty string
	 */
	String firstNonBlank(String... candidates) {
		for (String candidate : candidates) {
			if (StringUtils.hasText(candidate)) {
				return candidate;
			}
		}
		return "";
	}

	/**
	 * Converts null to an empty string.
	 *
	 * @param value possibly-null string
	 * @return the value, or empty string when null
	 */
	String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
