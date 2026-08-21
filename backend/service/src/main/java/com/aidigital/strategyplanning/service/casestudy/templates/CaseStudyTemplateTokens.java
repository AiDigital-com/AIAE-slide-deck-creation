package com.aidigital.strategyplanning.service.casestudy.templates;

import java.util.List;

/**
 * Canonical token registry for the Performance Case Study Google Slides template.
 * The deck filler replaces the {@code {{token}}} form of each key with its reviewed value.
 */
public final class CaseStudyTemplateTokens {

	/**
	 * All token keys of the Performance Case Study template.
	 */
	public static final List<String> TOKEN_KEYS = List.of(
			"client_vertical",
			"client_challenge",
			"priority_1",
			"priority_2",
			"priority_3",
			"solution_body",
			"results_intro",
			"result_detail_1",
			"result_detail_2",
			"result_detail_3",
			"metric_1_value",
			"metric_1_label",
			"metric_2_value",
			"metric_2_label",
			"metric_3_value",
			"metric_3_label");

	private CaseStudyTemplateTokens() {
	}
}
