package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerParagraphStyle;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerRunStyle;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextFitter;
import org.apache.poi.sl.usermodel.TextShape.TextAutofit;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for writing drafted text into template shapes while keeping their styling.
 */
class OnePagerTextWriterImplTest {

	private XSLFTextBox styledBox(XSLFSlide slide, String text) {
		XSLFTextBox box = slide.createTextBox();
		box.setAnchor(new Rectangle2D.Double(0, 0, 200, 40));
		box.setText(text);
		XSLFTextParagraph paragraph = box.getTextParagraphs().get(0);
		paragraph.setLineSpacing(150.0);
		paragraph.setIndent(4.0);
		paragraph.setLeftMargin(6.0);
		paragraph.setSpaceBefore(2.0);
		paragraph.setSpaceAfter(3.0);
		XSLFTextRun run = paragraph.getTextRuns().get(0);
		run.setFontFamily("Arial");
		run.setFontSize(14.0);
		run.setBold(true);
		run.setItalic(true);
		run.setFontColor(Color.RED);
		return box;
	}

	@Test
	void shouldCaptureTheFirstRunStyleAndInheritWhenThereIsNoneTest() throws IOException {
		// Given: a styled box, an empty box, and a non-text shape
		try (XMLSlideShow show = new XMLSlideShow()) {
			XSLFSlide slide = show.createSlide();
			XSLFTextBox styled = styledBox(slide, "Styled");
			XSLFTextBox empty = slide.createTextBox();
			empty.clearText();
			XSLFAutoShape shape = slide.createAutoShape();
			OnePagerTextWriterImpl writer = new OnePagerTextWriterImpl(mock(OnePagerTextFitter.class));

			// When: styles are captured
			OnePagerRunStyle captured = writer.captureStyle(styled);
			OnePagerParagraphStyle paragraphStyle = writer.captureParagraphStyle(styled);

			// Then: the styled run comes back in full and the others inherit everything
			assertThat(captured).isEqualTo(new OnePagerRunStyle("Arial", 14.0, true, true, Color.RED));
			assertThat(paragraphStyle).isEqualTo(new OnePagerParagraphStyle(4.0, 6.0, 150.0, 2.0, 3.0));
			assertThat(writer.captureStyle(styled, 5)).isEqualTo(OnePagerRunStyle.INHERITED);
			assertThat(writer.captureStyle(empty)).isEqualTo(OnePagerRunStyle.INHERITED);
			assertThat(writer.captureParagraphStyle(empty)).isEqualTo(OnePagerParagraphStyle.INHERITED);
			assertThat(writer.captureStyle(shape)).isEqualTo(OnePagerRunStyle.INHERITED);
		}
	}

	@Test
	void shouldRewriteLinesKeepingTheRunAndParagraphStyleTest() throws IOException {
		// Given: a styled box
		try (XMLSlideShow show = new XMLSlideShow()) {
			XSLFTextBox box = styledBox(show.createSlide(), "Old");
			OnePagerTextWriterImpl writer = new OnePagerTextWriterImpl(mock(OnePagerTextFitter.class));

			// When: two lines with markdown are written, then nothing, then a single line
			writer.setLines(box, List.of("**Bold** first", "second"));
			String twoLines = box.getText();
			XSLFTextRun firstRun = box.getTextParagraphs().get(0).getTextRuns().get(0);
			String fontFamily = firstRun.getFontFamily();
			Double fontSize = firstRun.getFontSize();
			boolean bold = firstRun.isBold();
			boolean italic = firstRun.isItalic();
			Double lineSpacing = box.getTextParagraphs().get(1).getLineSpacing();
			writer.setLines(box, null);
			String cleared = box.getText();
			writer.setSingleLine(box, null);
			String nullLine = box.getText();
			writer.setSingleLine(box, "Only");

			// Then: markdown is stripped and every paragraph keeps the master's styling
			assertThat(twoLines).isEqualTo("Bold first\nsecond");
			assertThat(fontFamily).isEqualTo("Arial");
			assertThat(fontSize).isEqualTo(14.0);
			assertThat(bold).isTrue();
			assertThat(italic).isTrue();
			assertThat(lineSpacing).isEqualTo(150.0);
			assertThat(cleared).isEmpty();
			assertThat(nullLine).isEmpty();
			assertThat(box.getText()).isEqualTo("Only");
		}
	}

	@Test
	void shouldWriteTwoDifferentlyStyledParagraphsTest() throws IOException {
		// Given: a styled box and two run styles
		try (XMLSlideShow show = new XMLSlideShow()) {
			XSLFTextBox box = styledBox(show.createSlide(), "Old");
			OnePagerTextWriterImpl writer = new OnePagerTextWriterImpl(mock(OnePagerTextFitter.class));
			OnePagerRunStyle label = new OnePagerRunStyle("Impact", 8.0, true, false, Color.BLUE);

			// When: the label and a sentence are written
			writer.setTwoParagraphs(box, "KEY INSIGHT", label, "_Sentence_ here", OnePagerRunStyle.INHERITED);

			// Then: each paragraph carries its own style and the paragraph layout survives
			List<XSLFTextParagraph> paragraphs = box.getTextParagraphs();
			assertThat(box.getText()).isEqualTo("KEY INSIGHT\nSentence here");
			assertThat(paragraphs.get(0).getTextRuns().get(0).getFontFamily()).isEqualTo("Impact");
			assertThat(paragraphs.get(0).getTextRuns().get(0).getFontSize()).isEqualTo(8.0);
			assertThat(paragraphs.get(1).getTextRuns().get(0).isBold()).isFalse();
			assertThat(paragraphs.get(1).getLineSpacing()).isEqualTo(150.0);
		}
	}

	@Test
	void shouldFitTextToTheShapeWidthAndSkipNonTextShapesTest() throws IOException {
		// Given: a styled box, a plain shape, and a fitter that shortens everything
		try (XMLSlideShow show = new XMLSlideShow()) {
			XSLFSlide slide = show.createSlide();
			XSLFTextBox box = styledBox(slide, "Old");
			XSLFTextBox unstyled = slide.createTextBox();
			unstyled.setAnchor(new Rectangle2D.Double(0, 0, 100, 20));
			unstyled.clearText();
			XSLFAutoShape plain = slide.createAutoShape();
			OnePagerTextFitter fitter = mock(OnePagerTextFitter.class);
			when(fitter.fitToWidth(anyString(), anyDouble(), eq("Arial"), eq(14.0))).thenReturn("Short");
			when(fitter.fitToWidth(anyString(), anyDouble(), eq(null), eq(9.0))).thenReturn("Fallback");
			OnePagerTextWriterImpl writer = new OnePagerTextWriterImpl(fitter);

			// When: text is fitted into each
			writer.setFittedText(box, "A very long value", 33.0);
			writer.setFittedText(unstyled, "Unstyled value", 9.0);
			writer.setFittedText(plain, "Ignored", 33.0);

			// Then: the box takes the fitted text measured at its own font, the fallback size is
			// used without a run style, and the plain shape is left alone
			assertThat(box.getText()).isEqualTo("Short");
			assertThat(unstyled.getText()).isEqualTo("Fallback");
			verify(fitter).fitToWidth(eq("A very long value"), anyDouble(), eq("Arial"), eq(14.0));
			assertThat(plain.getText()).isEmpty();
		}
	}

	@Test
	void shouldStripOnlyMarkdownEmphasisTest() {
		// Given: a writer with no fitter needs
		OnePagerTextFitter fitter = mock(OnePagerTextFitter.class);
		OnePagerTextWriterImpl writer = new OnePagerTextWriterImpl(fitter);

		// When-Then: emphasis markers go, plain text and null pass through untouched
		assertThat(writer.stripMarkdown("**bold** and *italic* and _under_ and `code`"))
				.isEqualTo("bold and italic and under and code");
		assertThat(writer.stripMarkdown("3 x 4 = 12")).isEqualTo("3 x 4 = 12");
		assertThat(writer.stripMarkdown("plain")).isEqualTo("plain");
		assertThat(writer.stripMarkdown(null)).isNull();
		verifyNoInteractions(fitter);
	}

	@Test
	void shouldMakeEveryWrittenBoxShrinkItsTextOnOverflowTest() throws IOException {
		// Given: boxes authored like the master, one without autofit and one that resizes to its text
		try (XMLSlideShow show = new XMLSlideShow()) {
			XSLFTextBox noAutofit = styledBox(show.createSlide(), "Old");
			noAutofit.setTextAutofit(TextAutofit.NONE);
			XSLFTextBox growsWithText = styledBox(show.createSlide(), "Old");
			growsWithText.setTextAutofit(TextAutofit.SHAPE);
			OnePagerTextWriterImpl writer = new OnePagerTextWriterImpl(mock(OnePagerTextFitter.class));

			// When: drafted copy is written into both, by lines and by two paragraphs
			writer.setLines(noAutofit, List.of("A much longer drafted line than the sample", "and another"));
			writer.setTwoParagraphs(growsWithText, "Title", writer.captureStyle(growsWithText),
					"Body copy that may run past the sample length", writer.captureStyle(growsWithText));

			// Then: both boxes shrink their text instead of overflowing or growing into the next module
			assertThat(noAutofit.getTextAutofit()).isEqualTo(TextAutofit.NORMAL);
			assertThat(growsWithText.getTextAutofit()).isEqualTo(TextAutofit.NORMAL);
			assertThat(noAutofit.getText()).startsWith("A much longer");
		}
	}
}
