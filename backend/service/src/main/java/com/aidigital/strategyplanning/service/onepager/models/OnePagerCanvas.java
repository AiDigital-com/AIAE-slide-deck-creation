package com.aidigital.strategyplanning.service.onepager.models;

import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The shell slide a one-pager is assembled onto, with the id counter handed to cloned shapes.
 *
 * <p>Every shape id already in the master is well under the counter's start (the master has under
 * 400 shapes), so clones never collide with an original shape or with each other even when the
 * same source shape is cloned more than once. The counter lives on this per-request object so
 * concurrent assemblies never share state.
 *
 * @param slide       the shell slide that receives the modules
 * @param nextShapeId id handed to the next cloned shape
 */
public record OnePagerCanvas(XSLFSlide slide, AtomicInteger nextShapeId) {

	/** First id handed to a cloned shape. */
	public static final int CLONE_ID_START = 100_000;

	/**
	 * Creates a canvas around a shell slide with a fresh id counter.
	 *
	 * @param slide the shell slide
	 */
	public OnePagerCanvas(XSLFSlide slide) {
		this(slide, new AtomicInteger(CLONE_ID_START));
	}
}
