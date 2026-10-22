package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudySummary;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconChoice;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconSlot;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageCaseStudyContentAssembler;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageIconLibraryService;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTemplateRegistry;
import com.aidigital.strategyplanning.service.casestudy.templates.OnePageCaseStudyTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link OnePageCaseStudyContentAssembler}.
 */
@Service
@RequiredArgsConstructor
public class OnePageCaseStudyContentAssemblerImpl implements OnePageCaseStudyContentAssembler {

	/** Title saved when neither title line was filled. */
	public static final String DEFAULT_TITLE = "One-Page Case Study";

	private static final String JOIN_SEPARATOR = ", ";
	private static final String TITLE_SEPARATOR = " ";
	private static final List<String> SOLUTION_KEYS = List.of("tactic_1_text", "tactic_2_text", "tactic_3_text",
			"tactic_4_text");
	private static final List<String> KEY_METRIC_KEYS = List.of(OnePageCaseStudyTemplate.KEY_HEADLINE_STAT,
			"stat_1_value", "stat_2_value", "stat_3_value");

	private final CaseStudyProperties properties;
	private final OnePageTemplateRegistry templateRegistry;
	private final OnePageIconLibraryService iconLibrary;

	@Override
	public Map<String, String> buildTokenValues(List<OnePageFieldValue> fields) {
		Map<String, String> reviewed = new LinkedHashMap<>();
		if (fields != null) {
			fields.forEach(field -> reviewed.put(field.key(), field.value()));
		}
		Map<String, String> tokens = new LinkedHashMap<>();
		for (OnePageFieldSpec spec : templateRegistry.fields()) {
			tokens.put(spec.key(), templateRegistry.formatValue(spec, reviewed.get(spec.key())));
		}
		return tokens;
	}

	@Override
	public Map<String, String> buildIconUrls(List<OnePageIconChoice> icons, String requestBaseUrl) {
		Map<String, String> urls = new LinkedHashMap<>();
		String baseUrl = StringUtils.hasText(properties.getPublicAssetBaseUrl())
				? properties.getPublicAssetBaseUrl() : requestBaseUrl;
		if (icons == null || !StringUtils.hasText(baseUrl)) {
			return urls;
		}
		Map<String, String> chosen = new LinkedHashMap<>();
		icons.forEach(icon -> chosen.put(icon.slot(), icon.icon()));
		for (OnePageIconSlot slot : templateRegistry.iconSlots()) {
			String name = chosen.get(slot.slot());
			if (iconLibrary.contains(name)) {
				urls.put(slot.slot(), iconLibrary.imageUrl(baseUrl, name));
			}
		}
		return urls;
	}

	@Override
	public OnePageCaseStudySummary summarize(Map<String, String> tokenValues) {
		String title = (tokenValues.getOrDefault(OnePageCaseStudyTemplate.KEY_TITLE_LINE_1, "") + TITLE_SEPARATOR
				+ tokenValues.getOrDefault(OnePageCaseStudyTemplate.KEY_TITLE_LINE_2, "")).trim();
		return new OnePageCaseStudySummary(
				title.isEmpty() ? DEFAULT_TITLE : title,
				blankToNull(tokenValues.get(OnePageCaseStudyTemplate.KEY_CLIENT)),
				blankToNull(tokenValues.get(OnePageCaseStudyTemplate.KEY_MAIN_TOPIC)),
				blankToNull(tokenValues.get(OnePageCaseStudyTemplate.KEY_CHALLENGE)),
				blankToNull(joinNonBlank(tokenValues, SOLUTION_KEYS)),
				blankToNull(tokenValues.get(OnePageCaseStudyTemplate.KEY_RESULTS_TEXT)),
				blankToNull(joinNonBlank(tokenValues, KEY_METRIC_KEYS)),
				blankToNull(tokenValues.get(OnePageCaseStudyTemplate.KEY_FLIGHT)));
	}

	/**
	 * Joins the non-blank token values for the given keys with ", ".
	 *
	 * @param tokens token values
	 * @param keys   keys to join, in order
	 * @return joined values, empty when all are blank
	 */
	String joinNonBlank(Map<String, String> tokens, List<String> keys) {
		List<String> values = new ArrayList<>();
		for (String key : keys) {
			String value = tokens.get(key);
			if (StringUtils.hasText(value)) {
				values.add(value);
			}
		}
		return String.join(JOIN_SEPARATOR, values);
	}

	/**
	 * Converts a blank string to null.
	 *
	 * @param value value to check
	 * @return the value, or null when blank
	 */
	String blankToNull(String value) {
		return StringUtils.hasText(value) ? value : null;
	}
}
