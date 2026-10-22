package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextMeasurement;

import java.util.List;
import java.util.Map;

/**
 * Measures a set of One-Page Case Study values against their boxes on the slide, looking up the
 * value that shares each box's line (a stat's value before its label) on the caller's behalf.
 */
public interface OnePageFieldMeasurer {

	/**
	 * Measures every template field and attaches its fit, plus a reviewer note for any value the
	 * source material could not stretch to fill its box.
	 *
	 * @param values values keyed by field; fields left out are measured as blank
	 * @return every field in slide order, with its fit
	 */
	List<OnePageCaseStudyField> measureFields(Map<String, String> values);

	/**
	 * Measures one field's current value in its box.
	 *
	 * @param spec   field with a box
	 * @param values current values keyed by field
	 * @return the measurement
	 */
	OnePageTextMeasurement measure(OnePageFieldSpec spec, Map<String, String> values);

	/**
	 * Measures a candidate value for a field in its box, with the current shared-line prefix.
	 *
	 * @param spec      field with a box
	 * @param candidate candidate value
	 * @param values    current values keyed by field (for the shared-line prefix)
	 * @return the measurement
	 */
	OnePageTextMeasurement measureCandidate(OnePageFieldSpec spec, String candidate, Map<String, String> values);

	/**
	 * Returns the value that shares a field's line (a stat's value before its label), or null.
	 *
	 * @param spec   field with a box
	 * @param values current values keyed by field
	 * @return the prefix value, or null
	 */
	String prefix(OnePageFieldSpec spec, Map<String, String> values);

	/**
	 * How far a candidate value is from fitting its box, in lines: 0 when it fits, the missing
	 * lines when short, the extra lines when over. Blank values never count as closer.
	 *
	 * @param spec      field with a box
	 * @param candidate candidate value
	 * @param values    current values keyed by field (for the shared-line prefix)
	 * @return distance in lines
	 */
	int distance(OnePageFieldSpec spec, String candidate, Map<String, String> values);

	/**
	 * Returns the measured fields whose value is short of or over its box (blank values excluded).
	 *
	 * @param values current values keyed by field
	 * @return fields that need a rewrite, in slide order
	 */
	List<OnePageFieldSpec> misfits(Map<String, String> values);
}
