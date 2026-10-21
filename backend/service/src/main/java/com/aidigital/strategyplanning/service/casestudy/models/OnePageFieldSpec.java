package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * One text field of the One-Page Case Study template.
 *
 * @param key      token key, written in the template as {@code {{key}}}
 * @param label    human-readable label for the review form
 * @param section  template section, used to group the review form
 * @param source   where the content may come from
 * @param maxChars hard character ceiling, a safety net behind the measured line limit
 * @param allCaps  whether the template sets this field in capitals
 * @param example  example value from the template's own case study (format only)
 * @param box      the measured box the field renders in, or null when it is not measured
 */
public record OnePageFieldSpec(String key, String label, String section, OnePageFieldSource source, int maxChars,
							   boolean allCaps, String example, OnePageTextBox box) {

	/**
	 * Reports whether the field has a measured box on the slide.
	 *
	 * @return true when the field's value is measured in lines rather than characters
	 */
	public boolean isMeasured() {
		return box != null;
	}
}
