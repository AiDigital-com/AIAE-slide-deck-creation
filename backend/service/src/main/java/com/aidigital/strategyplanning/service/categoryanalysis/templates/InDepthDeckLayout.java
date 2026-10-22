package com.aidigital.strategyplanning.service.categoryanalysis.templates;

import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Layout facts of the In-Depth Category Analysis Google Slides template.
 *
 * <p>Which slides carry charts, how those charts are tagged in the master, and the master's own
 * colours are properties of the template file rather than of any one service, so they live
 * together: a template redesign is a single edit.
 */
public final class InDepthDeckLayout {

	/**
	 * Slide whose donut chart is redrawn from the drafted values.
	 */
	public static final int DONUT_SLIDE = 8;

	/**
	 * Slide whose bar chart is resized to the drafted values.
	 */
	public static final int STAT_CHART_SLIDE = 18;

	/**
	 * Slide whose map graphic colours states as vector shapes. Only its headline, subheadline,
	 * and sources are tokenized; state-by-state recolouring is adjusted by hand per deck.
	 */
	public static final int REGULATORY_MAP_SLIDE = 23;

	/**
	 * Alt text tagging the master's donut (either the whole donut image or each of its slices).
	 */
	public static final String DONUT_ALT_TEXT = "{{donut_chart}}";

	/**
	 * Alt text pattern tagging one slice of a grouped donut, which Slides cannot tag as a whole.
	 */
	public static final Pattern DONUT_SLICE_ALT_TEXT = Pattern.compile("\\{\\{donut_slice_\\d+}}");

	/**
	 * Alt text of the legend dot whose fill colours donut slice N, with one {@code %d}.
	 */
	public static final String DONUT_SWATCH_ALT_TEXT_FORMAT = "{{donut_%d_swatch}}";

	/**
	 * Alt text of slide-18 bar N (1 = bottom bar), with one {@code %d}.
	 */
	public static final String BAR_ALT_TEXT_FORMAT = "{{bar_%d}}";

	/**
	 * Placeholder text of the value label sitting inside bar N's right end, with one {@code %d}.
	 */
	public static final String BAR_VALUE_PLACEHOLDER_FORMAT = "{{stat_chart_bar_%d_value}}";

	/**
	 * Token key of donut slice N's value, with one {@code %d}.
	 */
	public static final String DONUT_VALUE_KEY_FORMAT = "donut_%d_value";

	/**
	 * Token key of slide-18 bar N's value, with one {@code %d}.
	 */
	public static final String BAR_VALUE_KEY_FORMAT = "stat_chart_bar_%d_value";

	/**
	 * The master's slice colours (legend dots 1-4), used when a dot carries no readable fill.
	 */
	public static final List<Color> DEFAULT_DONUT_COLORS = List.of(
			new Color(0x43, 0x00, 0xDE), new Color(0xFF, 0x5C, 0xFB),
			new Color(0x55, 0x9F, 0xFF), new Color(0x99, 0xFD, 0x00));

	/**
	 * Shortest a slide-18 bar may shrink to, as a fraction of the longest master bar, so a bar
	 * with a tiny value stays visible.
	 */
	public static final double MIN_BAR_FRACTION = 0.06;

	/**
	 * Label shifts smaller than this many EMU are not worth a request.
	 */
	public static final double MIN_LABEL_SHIFT_EMU = 0.5;

	/**
	 * Object id prefix of the drawn donut image inserted into the new deck.
	 */
	public static final String DONUT_IMAGE_ID_PREFIX = "donut_";

	/**
	 * EMU per typographic point, for transforms the Slides API reports in PT.
	 */
	public static final double EMU_PER_POINT = 12700;

	private InDepthDeckLayout() {
	}
}
