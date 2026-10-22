package com.aidigital.strategyplanning.service.categoryanalysis.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Template a category-analysis row was generated from, persisted in the {@code template_kind}
 * column. One Page Generator pages are recorded in the same table so every deck tool shares one
 * "recent" list; the kind tells the UIs which rows belong to which tool.
 */
@Getter
@RequiredArgsConstructor
public enum CategoryAnalysisTemplateKind {

	/**
	 * The 3-5 slide Standard Category Analysis deck.
	 */
	STANDARD("STANDARD"),

	/**
	 * The 24-slide "check to include" In-Depth Category Analysis deck.
	 */
	IN_DEPTH("IN_DEPTH"),

	/**
	 * A One Page Generator page assembled from the one-pager master.
	 */
	ONE_PAGER("ONE_PAGER");

	/**
	 * Value stored in the {@code template_kind} column and returned on the API.
	 */
	private final String code;
}
