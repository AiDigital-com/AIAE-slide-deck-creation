package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextFitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link OnePagerTextFitter}, measuring with AWT font metrics in
 * headless mode.
 *
 * <p>Only the title's font ("Barlow Semi Condensed Black", bundled with the Case Study fonts) is
 * measured exactly. Other families (the KPI value's "Barlow Black") are measured with that same
 * bundled font enlarged by {@link #SUBSTITUTE_WIDTH_FACTOR}, because regular-width Barlow is wider
 * than Semi Condensed. The substitute is deliberately a bundled font and never a system font:
 * the logical sans-serif family resolves to a different physical font on every operating
 * system, which made the same text fit on a developer machine and overflow on the server. A wider
 * safety margin still hedges the remaining metric mismatch. Even the exact font is capped below
 * the full width: Google Slides' own kerning and hinting wrapped a line that AWT measured at 95%
 * of the box.
 */
@Slf4j
@Service
public class OnePagerTextFitterImpl implements OnePagerTextFitter {

	/** Fraction of the usable width a line may take when measured with the exact bundled font. */
	public static final double EXACT_FONT_SAFETY_MARGIN = 0.85;

	/** Fraction of the usable width a line may take when measured with a substitute font. */
	public static final double FALLBACK_SAFETY_MARGIN = 0.75;

	/** Classpath resource of the bundled title font. */
	public static final String TITLE_FONT_RESOURCE = "/fonts/case-study/BarlowSemiCondensed-Black.ttf";

	/** Family name the bundled title font reports, and the one the master uses for titles. */
	public static final String TITLE_FONT_FAMILY = "Barlow Semi Condensed Black";

	/**
	 * Size multiplier applied to the bundled font when measuring a family that is not bundled.
	 * Regular-width Barlow is roughly 12% wider than Barlow Semi Condensed, so the substitute is
	 * enlarged by that amount to avoid under-measuring.
	 */
	public static final double SUBSTITUTE_WIDTH_FACTOR = 1.12;

	private static final String HEADLESS_PROPERTY = "java.awt.headless";
	private static final String WORD_SEPARATOR = " ";
	private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);

	static {
		System.setProperty(HEADLESS_PROPERTY, Boolean.TRUE.toString());
	}

	private final Map<String, Font> exactFonts = new ConcurrentHashMap<>();
	private final Map<String, Font> derivedFonts = new ConcurrentHashMap<>();

	@Override
	public String fitToWidth(String text, double usableWidthPt, String fontFamily, double fontSizePt) {
		if (text == null || text.isEmpty() || usableWidthPt <= 0) {
			return text;
		}
		boolean exact = bundledFont(fontFamily).isPresent();
		Font font = resolveFont(fontFamily, fontSizePt);
		double maxWidthPt = usableWidthPt * (exact ? EXACT_FONT_SAFETY_MARGIN : FALLBACK_SAFETY_MARGIN);
		if (width(font, text) <= maxWidthPt) {
			return text;
		}
		String[] words = text.split(WORD_SEPARATOR);
		StringBuilder fitted = new StringBuilder();
		for (String word : words) {
			String candidate = fitted.isEmpty() ? word : fitted + WORD_SEPARATOR + word;
			if (width(font, candidate) > maxWidthPt) {
				break;
			}
			fitted.setLength(0);
			fitted.append(candidate);
		}
		String result = fitted.isEmpty() ? hardTruncate(words[0], font, maxWidthPt) : fitted.toString();
		log.warn("Drafted line overflowed its box width, truncated: \"{}\" -> \"{}\"", text, result);
		return result;
	}

	/**
	 * Truncates a single word to its longest prefix that fits, used when even the first word of a
	 * line overflows on its own.
	 *
	 * @param word       the word to truncate
	 * @param font       font to measure with
	 * @param maxWidthPt maximum allowed width in points
	 * @return the longest fitting prefix, or the first character when nothing fits
	 */
	String hardTruncate(String word, Font font, double maxWidthPt) {
		for (int length = word.length() - 1; length > 0; length--) {
			String candidate = word.substring(0, length);
			if (width(font, candidate) <= maxWidthPt) {
				return candidate;
			}
		}
		return word.isEmpty() ? word : word.substring(0, 1);
	}

	/**
	 * Measures a string's advance width.
	 *
	 * @param font font to measure with
	 * @param text text to measure
	 * @return width in points
	 */
	double width(Font font, String text) {
		return font.getStringBounds(text, CONTEXT).getWidth();
	}

	/**
	 * Resolves the font to measure with at the given size: the bundled exact font when the family
	 * matches, otherwise the bundled substitute from {@link #substituteFont(double)}. Derived
	 * fonts are cached per family and size.
	 *
	 * @param fontFamily font family read from the master shape, may be null
	 * @param fontSizePt font size in points
	 * @return the derived font
	 */
	Font resolveFont(String fontFamily, double fontSizePt) {
		String key = fontFamily + "@" + fontSizePt;
		return derivedFonts.computeIfAbsent(key, k -> bundledFont(fontFamily)
				.map(font -> font.deriveFont((float) fontSizePt))
				.orElseGet(() -> substituteFont(fontSizePt)));
	}

	/**
	 * Builds the font used to measure a family that is not bundled. It is the bundled title font
	 * enlarged by {@link #SUBSTITUTE_WIDTH_FACTOR}, so the result is identical on every operating
	 * system. Only when the bundled font cannot be loaded does it degrade to the platform's bold
	 * sans-serif.
	 *
	 * @param fontSizePt nominal font size in points
	 * @return the substitute font at the enlarged size
	 */
	Font substituteFont(double fontSizePt) {
		return bundledFont(TITLE_FONT_FAMILY)
				.map(font -> font.deriveFont((float) (fontSizePt * SUBSTITUTE_WIDTH_FACTOR)))
				.orElseGet(() -> new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont((float) fontSizePt));
	}

	/**
	 * Returns the bundled font whose family matches, loading the bundled title font on first use.
	 *
	 * @param fontFamily font family, may be null
	 * @return the bundled font, or empty when the family is not bundled
	 */
	Optional<Font> bundledFont(String fontFamily) {
		if (fontFamily == null) {
			return Optional.empty();
		}
		if (exactFonts.isEmpty()) {
			loadBundledFont(TITLE_FONT_RESOURCE).ifPresent(font -> exactFonts.put(font.getFamily(), font));
		}
		return Optional.ofNullable(exactFonts.get(fontFamily));
	}

	/**
	 * Loads a TrueType font from the classpath, logging and returning empty when the resource is
	 * missing or cannot be parsed so measuring degrades to the fallback font.
	 *
	 * @param resource classpath path of the font file
	 * @return the loaded font, or empty
	 */
	Optional<Font> loadBundledFont(String resource) {
		try (InputStream in = OnePagerTextFitterImpl.class.getResourceAsStream(resource)) {
			if (in == null) {
				log.warn("Bundled font resource not found: {}", resource);
				return Optional.empty();
			}
			return Optional.of(Font.createFont(Font.TRUETYPE_FONT, in));
		} catch (IOException | FontFormatException e) {
			log.warn("Could not load bundled font {}: {}", resource, e.getMessage());
			return Optional.empty();
		}
	}
}
