package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerRunStyle;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.util.List;

/**
 * Writes drafted text into cloned template shapes while keeping the master's run and paragraph
 * styling, and strips the markdown emphasis the model occasionally emits.
 */
public interface OnePagerTextWriter {

	/**
	 * Captures the run style of a shape's first paragraph.
	 *
	 * @param shape shape to read
	 * @return the captured style, inheriting everything when the shape has no text
	 */
	OnePagerRunStyle captureStyle(XSLFShape shape);

	/**
	 * Captures the run style of a specific paragraph, for shapes the master authors with two
	 * differently styled paragraphs (the insight cards' "KEY INSIGHT" label over a sentence).
	 *
	 * @param shape          shape to read
	 * @param paragraphIndex zero-based paragraph index
	 * @return the captured style, inheriting everything when unavailable
	 */
	OnePagerRunStyle captureStyle(XSLFShape shape, int paragraphIndex);

	/**
	 * Replaces a shape's text with one line.
	 *
	 * @param shape text shape to write into
	 * @param text  text to write; null writes an empty paragraph
	 */
	void setSingleLine(XSLFTextShape shape, String text);

	/**
	 * Replaces a shape's text with one paragraph per line, keeping the shape's first run style and
	 * paragraph layout.
	 *
	 * @param shape text shape to write into
	 * @param lines lines to write; null or empty writes a single empty paragraph
	 */
	void setLines(XSLFTextShape shape, List<String> lines);

	/**
	 * Replaces a shape's text with exactly two paragraphs, each with its own run style.
	 *
	 * @param shape       text shape to write into
	 * @param firstText   text of the first paragraph
	 * @param firstStyle  run style of the first paragraph
	 * @param secondText  text of the second paragraph
	 * @param secondStyle run style of the second paragraph
	 */
	void setTwoParagraphs(XSLFTextShape shape, String firstText, OnePagerRunStyle firstStyle, String secondText,
			OnePagerRunStyle secondStyle);

	/**
	 * Measures text against the shape's real width in the master's font and writes the longest
	 * prefix that fits, for boxes that never resize (titles, KPI values, chips).
	 *
	 * @param shape           target shape; ignored when it is not a text shape
	 * @param text            text to fit and write
	 * @param defaultFontSize font size used when the shape carries none of its own
	 */
	void setFittedText(XSLFShape shape, String text, double defaultFontSize);

	/**
	 * Removes markdown emphasis markers ({@code **bold**}, {@code *italic*}, {@code _italic_},
	 * {@code `code`}), keeping the inner text.
	 *
	 * @param text text to clean, may be null
	 * @return the text without emphasis markers
	 */
	String stripMarkdown(String text);
}
