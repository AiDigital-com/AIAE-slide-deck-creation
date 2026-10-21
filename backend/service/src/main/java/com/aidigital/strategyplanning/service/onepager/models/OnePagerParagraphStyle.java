package com.aidigital.strategyplanning.service.onepager.models;

/**
 * Paragraph layout captured from a master shape's first paragraph, re-applied to the paragraphs
 * written in its place. The master's bulleted card text uses 150% line spacing to line up with
 * separately cloned checkmark icons; rebuilding paragraphs without it packs the text tighter than
 * the icons expect.
 *
 * @param indent      first-line indent in points, or null to inherit
 * @param leftMargin  left margin in points, or null to inherit
 * @param lineSpacing line spacing, or null to inherit
 * @param spaceBefore space before the paragraph, or null to inherit
 * @param spaceAfter  space after the paragraph, or null to inherit
 */
public record OnePagerParagraphStyle(Double indent, Double leftMargin, Double lineSpacing, Double spaceBefore,
									 Double spaceAfter) {

	/** A layout that inherits everything from the shape. */
	public static final OnePagerParagraphStyle INHERITED = new OnePagerParagraphStyle(null, null, null, null, null);
}
