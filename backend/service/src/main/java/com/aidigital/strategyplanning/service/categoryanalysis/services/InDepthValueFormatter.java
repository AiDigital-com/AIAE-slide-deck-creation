package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;

import java.util.Map;

/**
 * Shapes In-Depth field values so they look like the master template's boxes around them.
 *
 * <p>Casing, closing periods, the "%" the master already prints, and the character budget of a
 * text box are all template facts; applying them in one place means a drafted value, a
 * fact-checked correction, and a hand edit all land on the slide the same way.
 */
public interface InDepthValueFormatter {

	/**
	 * Applies the master template's fixed styling rules to a field value — ALL CAPS for
	 * {@link com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens#ALL_CAPS_KEYS},
	 * a closing period for
	 * {@link com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens#SENTENCE_KEYS}.
	 *
	 * @param key   token key the value belongs to
	 * @param value field value, may be null
	 * @return the formatted value, or the input unchanged when no rule applies
	 */
	String formatForSlide(String key, String value);

	/**
	 * Formats a value for writing into the deck: {@link #formatForSlide} plus dropping the "%"
	 * from the fields whose master box already prints it.
	 *
	 * @param key   token key the value belongs to
	 * @param value field value, may be null
	 * @return the value as it should appear in the token's place
	 */
	String formatForDeck(String key, String value);

	/**
	 * Truncates a drafted value to the field's character budget regardless of whether the model
	 * honoured the prompt's stated limit, cutting at the last word boundary before the limit.
	 *
	 * @param spec  token spec the value belongs to
	 * @param value drafted value, already trimmed
	 * @return the value unchanged if within budget, otherwise truncated to fit
	 */
	String capToFit(InDepthTokenSpec spec, String value);

	/**
	 * Reports whether a value is a non-answer ("N/A", "-", "data not available", etc.) rather
	 * than real content, so a placeholder is treated exactly like a blank value.
	 *
	 * @param value candidate value, may be null
	 * @return true when the value is blank or a recognised placeholder
	 */
	boolean isPlaceholder(String value);

	/**
	 * Parses the leading number of a chart value such as "45%", "1,200" or "11.9M".
	 *
	 * @param value chart value text, may be null
	 * @return the parsed number, or 0 when the text holds no number
	 */
	double chartNumber(String value);

	/**
	 * Orders the chart series so the deck reads correctly: donut slices largest first (slice 1
	 * starts at 12 o'clock, then clockwise) and slide-18 bars smallest first (bar 1 = bottom
	 * bar). Labels move with their values; an incomplete series is left alone.
	 *
	 * @param values field values keyed by token key
	 * @return a copy of the values with each complete chart series reordered
	 */
	Map<String, String> sortChartSeries(Map<String, String> values);
}
