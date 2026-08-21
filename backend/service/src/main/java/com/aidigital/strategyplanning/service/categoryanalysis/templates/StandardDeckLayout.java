package com.aidigital.strategyplanning.service.categoryanalysis.templates;

/**
 * Layout facts of the Standard Category Analysis Google Slides template.
 *
 * <p>Slide numbers, artwork geometry, and the brand accent are properties of the template file
 * rather than of any one service, and both the image placement and the post-copy adjustments read
 * them. They live together so a template redesign is a single edit.
 */
public final class StandardDeckLayout {

	/**
	 * Slide whose artwork is a circular crop.
	 */
	public static final int CIRCULAR_IMAGE_SLIDE = 3;

	/**
	 * Slide whose artwork is scaled into a fixed frame.
	 */
	public static final int SCALED_IMAGE_SLIDE = 4;

	/**
	 * Slide whose artwork is fitted inside the slide bounds.
	 */
	public static final int FIT_INSIDE_SLIDE = 2;

	/**
	 * Size the scaled artwork is placed at (EMU).
	 */
	public static final double SCALED_TARGET_SIZE_EMU = 3977800;

	/**
	 * Absolute X offset of the scaled artwork (EMU).
	 */
	public static final double SCALED_TARGET_TRANSLATE_X = 2583100;

	/**
	 * Absolute Y offset of the scaled artwork (EMU).
	 */
	public static final double SCALED_TARGET_TRANSLATE_Y = 1722800;

	/**
	 * Size the circular artwork is placed at (EMU).
	 */
	public static final double CIRCLE_TARGET_SIZE_EMU = 4112400;

	/**
	 * Absolute X offset of the circular artwork (EMU).
	 */
	public static final double CIRCLE_TARGET_TRANSLATE_X = -508400;

	/**
	 * Absolute Y offset of the circular artwork (EMU).
	 */
	public static final double CIRCLE_TARGET_TRANSLATE_Y = 1668450;

	/**
	 * Heading text on the title slide, located by text because the template has no stable id.
	 */
	public static final String TITLE_HEADING_TEXT = "CATEGORY ANALYSIS";

	/**
	 * Heading text on the trends slide, located by text.
	 */
	public static final String TRENDING_HEADING_TEXT = "WHAT\u2019S TRENDING?";

	/**
	 * Font size the trends heading is restyled to (PT).
	 */
	public static final double TRENDING_HEADING_FONT_SIZE_PT = 18;

	/**
	 * Font size the trend bullets are restyled to (PT).
	 */
	public static final double TREND_BULLETS_FONT_SIZE_PT = 11;

	/**
	 * Red channel of the brand green accent.
	 */
	public static final double BRAND_GREEN_RED = 0xAE / 255.0;

	/**
	 * Green channel of the brand green accent.
	 */
	public static final double BRAND_GREEN_GREEN = 0xF3 / 255.0;

	/**
	 * Blue channel of the brand green accent.
	 */
	public static final double BRAND_GREEN_BLUE = 0x3E / 255.0;

	private StandardDeckLayout() {
	}
}
