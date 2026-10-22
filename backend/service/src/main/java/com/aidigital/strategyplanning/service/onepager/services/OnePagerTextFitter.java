package com.aidigital.strategyplanning.service.onepager.services;

/**
 * Measures drafted text against a template shape's real width in the master's font, instead of a
 * flat character-count guess: the three header shells size their title box very differently, so
 * the same string that fits one shell overflows another.
 */
public interface OnePagerTextFitter {

	/**
	 * Returns {@code text} unchanged when it fits the usable width at the given font, otherwise the
	 * longest word-boundary prefix that fits (or, when even the first word overflows, that word
	 * hard-truncated).
	 *
	 * @param text          candidate line, may be null
	 * @param usableWidthPt the shape's width minus its insets, in points
	 * @param fontFamily    the run's font family as read from the master shape
	 * @param fontSizePt    the run's font size in points
	 * @return the text that fits
	 */
	String fitToWidth(String text, double usableWidthPt, String fontFamily, double fontSizePt);
}
