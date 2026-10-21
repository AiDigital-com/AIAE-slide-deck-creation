package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * The box a One-Page Case Study field renders in on the slide, measured from the template: the
 * font file and size the template uses, the text width inside the box's insets (in points), and
 * how many wrapped lines the box takes. {@code minLines} is the fill target — fewer lines leave
 * the box visibly empty; {@code maxLines} is the most that fits before the text runs into the
 * next element.
 *
 * @param fontFile  font file under {@code fonts/case-study/}
 * @param fontSize  font size in points
 * @param width     text width in points (box width minus left and right insets)
 * @param minLines  fewest lines that fill the box (0 = no fill target)
 * @param maxLines  most lines that fit
 * @param prefixKey field whose value shares the line and is measured first (stat value before its
 *                  label), or null
 */
public record OnePageTextBox(String fontFile, double fontSize, double width, int minLines, int maxLines,
							 String prefixKey) {

}
