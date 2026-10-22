package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerParagraphStyle;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerRunStyle;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextFitter;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link OnePagerTextWriter}.
 */
@Service
@RequiredArgsConstructor
public class OnePagerTextWriterImpl implements OnePagerTextWriter {

	/**
	 * Matches {@code **bold**}, {@code *italic*}/{@code _italic_}, and {@code `code`}: the model
	 * occasionally emits these although its output is plain text for a Slides run, so a literal
	 * {@code **} would otherwise appear on the page.
	 */
	private static final Pattern MARKDOWN_EMPHASIS = Pattern.compile(
			"\\*\\*(.+?)\\*\\*|(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)|_(.+?)_|`(.+?)`");
	private static final String MARKDOWN_MARKERS = "*_`";
	private static final int FIRST_PARAGRAPH = 0;

	private final OnePagerTextFitter textFitter;

	@Override
	public OnePagerRunStyle captureStyle(XSLFShape shape) {
		return captureStyle(shape, FIRST_PARAGRAPH);
	}

	@Override
	public OnePagerRunStyle captureStyle(XSLFShape shape, int paragraphIndex) {
		if (!(shape instanceof XSLFTextShape textShape)) {
			return OnePagerRunStyle.INHERITED;
		}
		List<XSLFTextParagraph> paragraphs = textShape.getTextParagraphs();
		if (paragraphIndex >= paragraphs.size()) {
			return OnePagerRunStyle.INHERITED;
		}
		for (XSLFTextRun run : paragraphs.get(paragraphIndex).getTextRuns()) {
			return new OnePagerRunStyle(run.getFontFamily(), run.getFontSize(), run.isBold(), run.isItalic(),
					solidColor(run));
		}
		return OnePagerRunStyle.INHERITED;
	}

	/**
	 * Reads a run's solid font color. Theme colors that do not resolve to plain RGB are left null,
	 * so the new run inherits the shape's default color.
	 *
	 * @param run run to read
	 * @return the solid color, or null
	 */
	Color solidColor(XSLFTextRun run) {
		try {
			PaintStyle paint = run.getFontColor();
			return paint instanceof PaintStyle.SolidPaint solid ? solid.getSolidColor().getColor() : null;
		} catch (RuntimeException e) {
			return null;
		}
	}

	/**
	 * Captures the paragraph layout the rebuilt paragraphs must keep.
	 *
	 * @param shape text shape whose first paragraph's layout is captured
	 * @return the captured layout, inheriting everything when the shape has no paragraphs
	 */
	OnePagerParagraphStyle captureParagraphStyle(XSLFTextShape shape) {
		List<XSLFTextParagraph> paragraphs = shape.getTextParagraphs();
		if (paragraphs.isEmpty()) {
			return OnePagerParagraphStyle.INHERITED;
		}
		XSLFTextParagraph paragraph = paragraphs.get(FIRST_PARAGRAPH);
		return new OnePagerParagraphStyle(paragraph.getIndent(), paragraph.getLeftMargin(),
				paragraph.getLineSpacing(), paragraph.getSpaceBefore(), paragraph.getSpaceAfter());
	}

	/**
	 * Applies a captured run style; null properties are left untouched.
	 *
	 * @param run   run to style
	 * @param style style to apply
	 */
	void applyStyle(XSLFTextRun run, OnePagerRunStyle style) {
		if (style.fontFamily() != null) {
			run.setFontFamily(style.fontFamily());
		}
		if (style.fontSize() != null) {
			run.setFontSize(style.fontSize());
		}
		run.setBold(style.bold());
		run.setItalic(style.italic());
		if (style.color() != null) {
			run.setFontColor(style.color());
		}
	}

	/**
	 * Applies a captured paragraph layout; null properties are left untouched.
	 *
	 * @param paragraph paragraph to style
	 * @param style     layout to apply
	 */
	void applyParagraphStyle(XSLFTextParagraph paragraph, OnePagerParagraphStyle style) {
		if (style.indent() != null) {
			paragraph.setIndent(style.indent());
		}
		if (style.leftMargin() != null) {
			paragraph.setLeftMargin(style.leftMargin());
		}
		if (style.lineSpacing() != null) {
			paragraph.setLineSpacing(style.lineSpacing());
		}
		if (style.spaceBefore() != null) {
			paragraph.setSpaceBefore(style.spaceBefore());
		}
		if (style.spaceAfter() != null) {
			paragraph.setSpaceAfter(style.spaceAfter());
		}
	}

	/**
	 * Appends one styled paragraph holding one run.
	 *
	 * @param shape          text shape to append to
	 * @param text           paragraph text, null writes empty
	 * @param runStyle       run style
	 * @param paragraphStyle paragraph layout
	 */
	void addParagraph(XSLFTextShape shape, String text, OnePagerRunStyle runStyle,
			OnePagerParagraphStyle paragraphStyle) {
		XSLFTextParagraph paragraph = shape.addNewTextParagraph();
		applyParagraphStyle(paragraph, paragraphStyle);
		XSLFTextRun run = paragraph.addNewTextRun();
		run.setText(text == null ? "" : stripMarkdown(text));
		applyStyle(run, runStyle);
	}

	@Override
	public void setSingleLine(XSLFTextShape shape, String text) {
		setLines(shape, text == null ? List.of("") : List.of(text));
	}

	@Override
	public void setLines(XSLFTextShape shape, List<String> lines) {
		OnePagerRunStyle style = captureStyle(shape);
		OnePagerParagraphStyle paragraphStyle = captureParagraphStyle(shape);
		shape.clearText();
		List<String> effectiveLines = lines == null || lines.isEmpty() ? List.of("") : lines;
		for (String line : effectiveLines) {
			addParagraph(shape, line, style, paragraphStyle);
		}
	}

	@Override
	public void setTwoParagraphs(XSLFTextShape shape, String firstText, OnePagerRunStyle firstStyle,
			String secondText, OnePagerRunStyle secondStyle) {
		OnePagerParagraphStyle paragraphStyle = captureParagraphStyle(shape);
		shape.clearText();
		addParagraph(shape, firstText, firstStyle, paragraphStyle);
		addParagraph(shape, secondText, secondStyle, paragraphStyle);
	}

	@Override
	public void setFittedText(XSLFShape shape, String text, double defaultFontSize) {
		if (!(shape instanceof XSLFTextShape textShape)) {
			return;
		}
		OnePagerRunStyle style = captureStyle(shape);
		double fontSize = style.fontSize() != null ? style.fontSize() : defaultFontSize;
		double usableWidth = textShape.getAnchor().getWidth() - textShape.getLeftInset() - textShape.getRightInset();
		setSingleLine(textShape, textFitter.fitToWidth(text, usableWidth, style.fontFamily(), fontSize));
	}

	@Override
	public String stripMarkdown(String text) {
		if (text == null || text.chars().noneMatch(c -> MARKDOWN_MARKERS.indexOf(c) >= 0)) {
			return text;
		}
		Matcher matcher = MARKDOWN_EMPHASIS.matcher(text);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String inner = matcher.group(1) != null ? matcher.group(1)
					: matcher.group(2) != null ? matcher.group(2)
					: matcher.group(3) != null ? matcher.group(3)
					: matcher.group(4);
			matcher.appendReplacement(result, Matcher.quoteReplacement(inner));
		}
		matcher.appendTail(result);
		return result.toString();
	}
}
