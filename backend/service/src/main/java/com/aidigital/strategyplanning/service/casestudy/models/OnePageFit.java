package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * How a One-Page Case Study value sits in its text box on the slide.
 */
public enum OnePageFit {

	/** Fills the box without overflowing. */
	FITS,

	/** Leaves part of the box empty. */
	SHORT,

	/** Runs past the box into the next element. */
	OVER,

	/** Empty — the box is left blank on purpose. */
	EMPTY
}
