package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * Where a One-Page Case Study field's content may come from.
 */
public enum OnePageFieldSource {

	/** Facts taken only from the uploaded documents. */
	FILES,

	/** Short wording (titles, labels) the AI may write for flow. */
	WRITTEN
}
