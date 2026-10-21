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
 * measured exactly; other families (the KPI value's "Barlow Black") fall back to a bold
 * sans-serif, so a wider safety margin hedges the metric mismatch. Even the exact font is capped
 * below the full width: Google Slides' own kerning and hinting wrapped a line that AWT measured
 * at 95% of the box.
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
	 * matches, otherwise a bold sans-serif fallback. Derived fonts are cached per family and size.
	 *
	 * @param fontFamily font family read from the master shape, may be null
	 * @param fontSizePt font size in points
	 * @return the derived font
	 */
	Font resolveFont(String fontFamily, double fontSizePt) {
		String key = fontFamily + "@" + fontSizePt;
		return derivedFonts.computeIfAbsent(key, k -> bundledFont(fontFamily)
				.orElseGet(() -> new Font(Font.SANS_SERIF, Font.BOLD, 1))
				.deriveFont((float) fontSizePt));
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
