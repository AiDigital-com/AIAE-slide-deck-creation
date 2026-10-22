package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthTemplateTokenRegistry;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthValueFormatter;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthDeckLayout;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link InDepthValueFormatter}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InDepthValueFormatterImpl implements InDepthValueFormatter {

	/**
	 * Normalised non-answers that must never land on a slide as literal text.
	 */
	private static final Set<String> PLACEHOLDER_VALUES = Set.of(
			"-", "--", "—", "n/a", "na", "none", "unknown", "not available", "not applicable",
			"no data", "no data available", "data not available", "data unavailable", "unavailable",
			"unable to verify", "cannot verify", "could not verify", "tbd", "pending", "missing",
			"missing data", "not found", "no information", "insufficient data", "null", "undefined");

	private static final Pattern CHART_NUMBER = Pattern.compile("-?\\d[\\d,]*(\\.\\d+)?");
	private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.!]+$");
	private static final Pattern TRAILING_PERCENT = Pattern.compile("\\s*%+\\s*$");
	private static final Pattern SENTENCE_END = Pattern.compile(".*[.!?]$");
	private static final String DONUT_LABEL_KEY_FORMAT = "donut_%d_label";
	private static final String BAR_LABEL_KEY_FORMAT = "stat_chart_bar_%d_label";

	private final InDepthTemplateTokenRegistry tokenRegistry;

	@Override
	public String formatForSlide(String key, String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		String formatted = value.trim();
		if (InDepthTemplateTokens.ALL_CAPS_KEYS.contains(key)) {
			formatted = formatted.toUpperCase(Locale.ROOT);
		}
		if (InDepthTemplateTokens.SENTENCE_KEYS.contains(key) && !SENTENCE_END.matcher(formatted).matches()) {
			formatted = formatted + ".";
		}
		return formatted;
	}

	@Override
	public String formatForDeck(String key, String value) {
		String formatted = formatForSlide(key, value);
		if (formatted != null && InDepthTemplateTokens.PERCENT_SIGN_IN_MASTER_KEYS.contains(key)) {
			formatted = TRAILING_PERCENT.matcher(formatted).replaceAll("");
		}
		return formatted;
	}

	@Override
	public String capToFit(InDepthTokenSpec spec, String value) {
		int max = tokenRegistry.maxCharsFor(spec);
		if (value.length() <= max) {
			return value;
		}
		String cut = value.substring(0, max);
		int lastSpace = cut.lastIndexOf(' ');
		if (lastSpace > max / 2) {
			cut = cut.substring(0, lastSpace);
		}
		log.warn("Truncated drafted value for {} from {} to {} characters to fit its text box",
				spec.key(), value.length(), cut.length());
		return cut.trim();
	}

	@Override
	public boolean isPlaceholder(String value) {
		if (!StringUtils.hasText(value)) {
			return true;
		}
		String normalized = TRAILING_PUNCTUATION.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("");
		return PLACEHOLDER_VALUES.contains(normalized);
	}

	@Override
	public double chartNumber(String value) {
		if (value == null) {
			return 0;
		}
		Matcher matcher = CHART_NUMBER.matcher(value);
		return matcher.find() ? Double.parseDouble(matcher.group().replace(",", "")) : 0;
	}

	@Override
	public Map<String, String> sortChartSeries(Map<String, String> values) {
		Map<String, String> sorted = new LinkedHashMap<>(values);
		reorderSeries(sorted, DONUT_LABEL_KEY_FORMAT, InDepthDeckLayout.DONUT_VALUE_KEY_FORMAT,
				InDepthTemplateTokens.DONUT_SLICES, false);
		reorderSeries(sorted, BAR_LABEL_KEY_FORMAT, InDepthDeckLayout.BAR_VALUE_KEY_FORMAT,
				InDepthTemplateTokens.STAT_CHART_BARS, true);
		return sorted;
	}

	/**
	 * Rewrites one numbered label/value series in place so its items run in value order. Only
	 * applied when every item of the series is present, so a partial selection is left alone.
	 *
	 * @param values    field values, modified in place
	 * @param labelKey  label key pattern with one {@code %d} for the item number
	 * @param valueKey  value key pattern with one {@code %d} for the item number
	 * @param count     number of items in the series
	 * @param ascending true for smallest first, false for largest first
	 */
	void reorderSeries(Map<String, String> values, String labelKey, String valueKey, int count,
			boolean ascending) {
		List<String[]> items = new ArrayList<>();
		for (int n = 1; n <= count; n++) {
			String label = values.get(String.format(labelKey, n));
			String value = values.get(String.format(valueKey, n));
			if (label == null || value == null) {
				return;
			}
			items.add(new String[] {label, value});
		}
		Comparator<String[]> byValue = Comparator.comparingDouble(item -> chartNumber(item[1]));
		items.sort(ascending ? byValue : byValue.reversed());
		for (int n = 1; n <= count; n++) {
			values.put(String.format(labelKey, n), items.get(n - 1)[0]);
			values.put(String.format(valueKey, n), items.get(n - 1)[1]);
		}
	}
}
