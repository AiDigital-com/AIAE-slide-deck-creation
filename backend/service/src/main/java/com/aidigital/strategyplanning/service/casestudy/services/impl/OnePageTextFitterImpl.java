package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextBox;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextMeasurement;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTextFitter;
import org.springframework.stereotype.Service;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link OnePageTextFitter}, measuring with AWT font metrics in
 * headless mode.
 */
@Service
public class OnePageTextFitterImpl implements OnePageTextFitter {

	/** Narrow end of the width band, relative to the measured box width. */
	public static final double NARROW = 1.0;

	/** Wide end of the width band that covers Google Slides' rendering. */
	public static final double WIDE = 1.03;

	/** Classpath folder the template's font files are bundled under. */
	public static final String FONT_RESOURCE_PATH = "/fonts/case-study/";

	private static final String HEADLESS_PROPERTY = "java.awt.headless";
	private static final String WHITESPACE_RUN = "\\s+";
	private static final String SINGLE_SPACE = " ";
	private static final String TRAILING_PUNCTUATION = "[\\s,;:\\-–—]+$";
	private static final String ENDS_WITH_SENTENCE_MARK = "(?s).*[.!?]$";
	private static final String SENTENCE_END_PERIOD = ". ";
	private static final String SENTENCE_END_EXCLAMATION = "! ";
	private static final String SENTENCE_END_QUESTION = "? ";
	private static final String PERIOD = ".";
	private static final String CHARS_PER_LINE_SAMPLE =
			"Reallocated budget into the strongest formats and markets to cut acquisition costs";
	private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);

	/** Words a cut sentence must not end on. */
	private static final Set<String> DANGLING_WORDS = Set.of(
			"a", "an", "the", "and", "or", "but", "to", "of", "for", "with", "by", "in", "on", "at", "from",
			"while", "without", "into", "as", "that", "which", "its", "their", "our");

	static {
		System.setProperty(HEADLESS_PROPERTY, Boolean.TRUE.toString());
	}

	private final Map<String, Font> fonts = new ConcurrentHashMap<>();

	@Override
	public OnePageTextMeasurement measure(OnePageFieldSpec spec, String value, String prefix) {
		OnePageTextBox box = spec.box();
		if (value == null || value.isBlank()) {
			return new OnePageTextMeasurement(OnePageFit.EMPTY, 0, 0, box.minLines(), box.maxLines());
		}
		String text = prefix == null || prefix.isBlank() ? value : prefix + SINGLE_SPACE + value;
		int lines = wrap(text, box, NARROW).size();
		int wideLines = wrap(text, box, WIDE).size();
		OnePageFit fit = lines > box.maxLines() ? OnePageFit.OVER
				: wideLines < box.minLines() ? OnePageFit.SHORT : OnePageFit.FITS;
		return new OnePageTextMeasurement(fit, lines, wideLines, box.minLines(), box.maxLines());
	}

	@Override
	public List<String> wrap(String text, OnePageTextBox box, double scale) {
		Font font = font(box.fontFile()).deriveFont((float) box.fontSize());
		double width = box.width() * scale;
		List<String> lines = new ArrayList<>();
		String current = "";
		for (String word : text.trim().split(WHITESPACE_RUN)) {
			String candidate = current.isEmpty() ? word : current + SINGLE_SPACE + word;
			if (current.isEmpty() || font.getStringBounds(candidate, CONTEXT).getWidth() <= width) {
				current = candidate;
			} else {
				lines.add(current);
				current = word;
			}
		}
		lines.add(current);
		return lines;
	}

	@Override
	public String trimSentencesToFit(OnePageFieldSpec spec, String value, String prefix) {
		String trimmed = value;
		while (measure(spec, trimmed, prefix).fit() == OnePageFit.OVER) {
			int end = Math.max(trimmed.lastIndexOf(SENTENCE_END_PERIOD, trimmed.length() - 2),
					Math.max(trimmed.lastIndexOf(SENTENCE_END_EXCLAMATION),
							trimmed.lastIndexOf(SENTENCE_END_QUESTION)));
			if (end <= 0) {
				return value;
			}
			trimmed = trimmed.substring(0, end + 1).trim();
		}
		return measure(spec, trimmed, prefix).fit() == OnePageFit.FITS ? trimmed : value;
	}

	@Override
	public String trimToFit(OnePageFieldSpec spec, String value, String prefix) {
		if (measure(spec, value, prefix).fit() != OnePageFit.OVER) {
			return value;
		}
		String bySentence = trimSentencesToFit(spec, value, prefix);
		if (measure(spec, bySentence, prefix).fit() != OnePageFit.OVER) {
			return bySentence;
		}
		boolean sentence = value.matches(ENDS_WITH_SENTENCE_MARK);
		String trimmed = value;
		while (trimmed.contains(SINGLE_SPACE)) {
			trimmed = dropLastWord(trimmed);
			while (trimmed.contains(SINGLE_SPACE) && DANGLING_WORDS.contains(lastWord(trimmed))) {
				trimmed = dropLastWord(trimmed);
			}
			String candidate = sentence && !trimmed.matches(ENDS_WITH_SENTENCE_MARK) ? trimmed + PERIOD : trimmed;
			if (measure(spec, candidate, prefix).fit() != OnePageFit.OVER) {
				return candidate;
			}
		}
		return trimmed;
	}

	@Override
	public int charsPerLine(OnePageFieldSpec spec) {
		String sample = spec.allCaps() ? CHARS_PER_LINE_SAMPLE.toUpperCase(Locale.ROOT) : CHARS_PER_LINE_SAMPLE;
		Font font = font(spec.box().fontFile()).deriveFont((float) spec.box().fontSize());
		double perChar = font.getStringBounds(sample, CONTEXT).getWidth() / sample.length();
		return (int) Math.max(1, Math.round(spec.box().width() / perChar));
	}

	/**
	 * Removes the last word of a text along with any punctuation left dangling before it.
	 *
	 * @param text text with at least one space
	 * @return the text without its last word
	 */
	String dropLastWord(String text) {
		return text.substring(0, text.lastIndexOf(' ')).replaceAll(TRAILING_PUNCTUATION, "").trim();
	}

	/**
	 * Returns the last word of a text, lower-cased for the dangling-word check.
	 *
	 * @param text text with at least one space
	 * @return its last word
	 */
	String lastWord(String text) {
		return text.substring(text.lastIndexOf(' ') + 1).toLowerCase(Locale.ROOT);
	}

	/**
	 * Loads (once) a bundled font file.
	 *
	 * @param fileName font file under {@code fonts/case-study/}
	 * @return the font at its default size
	 */
	Font font(String fileName) {
		return fonts.computeIfAbsent(fileName, name -> {
			try (InputStream in = OnePageTextFitterImpl.class.getResourceAsStream(FONT_RESOURCE_PATH + name)) {
				if (in == null) {
					throw new IllegalStateException("Missing bundled font " + name);
				}
				return Font.createFont(Font.TRUETYPE_FONT, in);
			} catch (IOException e) {
				throw new UncheckedIOException("Could not read bundled font " + name, e);
			} catch (FontFormatException e) {
				throw new IllegalStateException("Bundled font is not a TrueType font: " + name, e);
			}
		});
	}
}
