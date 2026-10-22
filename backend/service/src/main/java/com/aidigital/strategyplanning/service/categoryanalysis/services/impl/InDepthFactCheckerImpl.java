package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDraftParser;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDraftPromptComposer;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthFactChecker;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthOpenAiGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthValueFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link InDepthFactChecker}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InDepthFactCheckerImpl implements InDepthFactChecker {

	private static final String SOURCES_KEY_SUFFIX = "_sources";

	private final InDepthOpenAiGateway openAiGateway;
	private final InDepthDraftPromptComposer promptComposer;
	private final InDepthDraftParser draftParser;
	private final InDepthValueFormatter valueFormatter;

	@Override
	public List<InDepthDraftField> verifyFactualFields(String category, List<InDepthTokenSpec> tokens,
	                                                    List<InDepthDraftField> drafted) {
		Map<String, InDepthDraftField> draftedByKey = new LinkedHashMap<>();
		for (InDepthDraftField field : drafted) {
			draftedByKey.put(field.key(), field);
		}
		Map<String, InDepthTokenSpec> specByKey = new LinkedHashMap<>();
		for (InDepthTokenSpec spec : tokens) {
			specByKey.put(spec.key(), spec);
		}
		List<InDepthTokenSpec> factual = tokens.stream().filter(this::isFactual).toList();
		if (factual.isEmpty()) {
			return drafted;
		}
		Map<String, String> corrections = new LinkedHashMap<>();
		for (List<InDepthTokenSpec> group : groupBySlideNumbers(factual)) {
			try {
				corrections.putAll(verifyGroup(category, group, draftedByKey));
			} catch (RuntimeException e) {
				log.warn("Fact-check failed for a group of {} field(s), keeping drafted values: {}",
						group.size(), e.getMessage());
			}
		}
		if (corrections.isEmpty()) {
			return drafted;
		}
		List<InDepthDraftField> result = new ArrayList<>();
		for (InDepthDraftField field : drafted) {
			String corrected = corrections.get(field.key());
			InDepthTokenSpec spec = specByKey.get(field.key());
			if (!StringUtils.hasText(corrected) || spec == null) {
				result.add(field);
				continue;
			}
			String finalValue = valueFormatter.formatForSlide(spec.key(),
					valueFormatter.capToFit(spec, corrected.trim()));
			result.add(new InDepthDraftField(field.key(), field.label(), field.slideNumbers(), finalValue));
		}
		return result;
	}

	/**
	 * Reports whether a field is a quantitative claim worth fact-checking rather than qualitative
	 * strategy text: sources lines always qualify, plus any field whose master sample text
	 * contains a digit, which captures stat values, chart data points, and dated labels.
	 *
	 * @param spec token spec to classify
	 * @return true when this field should be checked against live search
	 */
	boolean isFactual(InDepthTokenSpec spec) {
		if (spec.key().endsWith(SOURCES_KEY_SUFFIX)) {
			return true;
		}
		return spec.sampleText().chars().anyMatch(Character::isDigit);
	}

	/**
	 * Fact-checks one topic group's factual fields in a single {@code web_search} call.
	 *
	 * @param category     market category being analysed
	 * @param group        the topic's factual token specs to verify
	 * @param draftedByKey drafted values, keyed by token key
	 * @return corrections keyed by token key; a key is absent when the model returned a placeholder
	 */
	Map<String, String> verifyGroup(String category, List<InDepthTokenSpec> group,
			Map<String, InDepthDraftField> draftedByKey) {
		String body = promptComposer.buildVerifyRequestBody(category, group, draftedByKey);
		return draftParser.parseCorrections(draftParser.parseJson(openAiGateway.callResponses(body)), group);
	}

	/**
	 * Splits a flat token list into its distinct topic groups by slide-number list, preserving
	 * first-appearance order. Tokens sharing the same slide numbers are one topic and are always
	 * fact-checked together in one call.
	 *
	 * @param tokens token specs to group
	 * @return each distinct topic's tokens, in the order that topic first appears in the input
	 */
	List<List<InDepthTokenSpec>> groupBySlideNumbers(List<InDepthTokenSpec> tokens) {
		Map<List<Integer>, List<InDepthTokenSpec>> byGroup = new LinkedHashMap<>();
		for (InDepthTokenSpec spec : tokens) {
			byGroup.computeIfAbsent(spec.slideNumbers(), key -> new ArrayList<>()).add(spec);
		}
		return new ArrayList<>(byGroup.values());
	}
}
