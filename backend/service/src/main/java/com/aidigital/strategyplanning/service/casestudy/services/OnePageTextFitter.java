package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextBox;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextMeasurement;

import java.util.List;

/**
 * Measures how many lines a value wraps to inside its One-Page Case Study box, using the same
 * font files the template uses (bundled under {@code fonts/case-study/}) and Google Slides'
 * greedy word wrap, and shortens values that overflow.
 *
 * <p>Checked against decks Google Slides rendered, the measured width can be off by about 1% of
 * the box. So a value is judged against both ends of that band: it <em>fits</em> when it stays
 * within the box's line limit at the narrow end, and it <em>fills</em> when it reaches the box's
 * fill target at the wide end. Either way the verdict holds whatever the true width.
 */
public interface OnePageTextFitter {

	/**
	 * Measures a formatted value against its field's box.
	 *
	 * @param spec   field (must have a box)
	 * @param value  formatted value
	 * @param prefix value of the box's prefix field (stat value), or null
	 * @return the measurement
	 */
	OnePageTextMeasurement measure(OnePageFieldSpec spec, String value, String prefix);

	/**
	 * Wraps text into the box the way Google Slides does: words are added to a line while the
	 * line still fits the width; a single word wider than the box takes a line of its own.
	 *
	 * @param text  text to wrap
	 * @param box   measured box
	 * @param scale width multiplier (the narrow or the wide end of the rendering band)
	 * @return wrapped lines
	 */
	List<String> wrap(String text, OnePageTextBox box, double scale);

	/**
	 * Shortens an overflowing value by dropping whole trailing sentences, as long as what remains
	 * still fills the box. Never cuts inside a sentence.
	 *
	 * @param spec   field (must have a box)
	 * @param value  formatted value
	 * @param prefix value of the box's prefix field, or null
	 * @return the shortened value when whole sentences could make it fit; otherwise the value unchanged
	 */
	String trimSentencesToFit(OnePageFieldSpec spec, String value, String prefix);

	/**
	 * Last-resort guarantee that a value fits its box: whole sentences first; otherwise words are
	 * cut from the end, connector words ("to", "and", ...) are not left dangling, and text that
	 * was a sentence is closed with a period so it never reads as cut off mid-thought.
	 *
	 * @param spec   field (must have a box)
	 * @param value  formatted value
	 * @param prefix value of the box's prefix field, or null
	 * @return the value, shortened when it overflowed
	 */
	String trimToFit(OnePageFieldSpec spec, String value, String prefix);

	/**
	 * Estimates how many characters of typical copy fill one line of a box, used to tell the AI
	 * roughly how much to add or cut.
	 *
	 * @param spec field (must have a box)
	 * @return characters per line
	 */
	int charsPerLine(OnePageFieldSpec spec);
}
