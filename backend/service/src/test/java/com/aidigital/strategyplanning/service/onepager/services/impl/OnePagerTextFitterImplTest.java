package com.aidigital.strategyplanning.service.onepager.services.impl;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Unit tests for width-based text fitting with the bundled title font.
 */
class OnePagerTextFitterImplTest {

	private static final String TITLE_FONT = "Barlow Semi Condensed Black";
	private static final double TITLE_SIZE = 40.0;

	@Test
	void shouldPassShortOrEmptyTextThroughUnchangedTest() {
		// Given: a fitter and a wide box
		OnePagerTextFitterImpl fitter = new OnePagerTextFitterImpl();

		// When-Then: nothing is cut when it fits, is empty, or the box has no width
		assertThat(fitter.fitToWidth("SHORT", 500, TITLE_FONT, TITLE_SIZE)).isEqualTo("SHORT");
		assertThat(fitter.fitToWidth("", 500, TITLE_FONT, TITLE_SIZE)).isEmpty();
		assertThat(fitter.fitToWidth(null, 500, TITLE_FONT, TITLE_SIZE)).isNull();
		assertThat(fitter.fitToWidth("ANYTHING AT ALL", 0, TITLE_FONT, TITLE_SIZE)).isEqualTo("ANYTHING AT ALL");
	}

	@Test
	void shouldCutAtAWordBoundaryWhenALineOverflowsTest() {
		// Given: a narrow title box
		OnePagerTextFitterImpl fitter = new OnePagerTextFitterImpl();

		// When: a long line is fitted with the exact font and with an unknown font
		String exact = fitter.fitToWidth("SOLVING AUTOMOTIVE'S FRAGMENTATION", 230, TITLE_FONT, TITLE_SIZE);
		String fallback = fitter.fitToWidth("SOLVING AUTOMOTIVE'S FRAGMENTATION", 230, "Barlow Black", TITLE_SIZE);

		// Then: both keep a prefix of the line, and the substitute font is held to a tighter margin
		assertThat(exact).isIn("SOLVING", "SOLVING AUTOMOTIVE'S");
		assertThat("SOLVING AUTOMOTIVE'S FRAGMENTATION").startsWith(exact);
		assertThat(fallback).isNotEmpty();
		assertThat("SOLVING AUTOMOTIVE'S FRAGMENTATION").startsWith(fallback);
		assertThat(fallback.length()).isLessThanOrEqualTo(exact.length());
	}

	@Test
	void shouldHardTruncateASingleWordThatOverflowsOnItsOwnTest() {
		// Given: a box narrower than one word
		OnePagerTextFitterImpl fitter = new OnePagerTextFitterImpl();

		// When: a single long word is fitted
		String fitted = fitter.fitToWidth("FRAGMENTATION", 60, TITLE_FONT, TITLE_SIZE);
		Font font = fitter.resolveFont(TITLE_FONT, TITLE_SIZE);

		// Then: the longest prefix that fits is kept, never less than one character
		assertThat(fitted).isNotEmpty();
		assertThat("FRAGMENTATION").startsWith(fitted);
		assertThat(fitter.width(font, fitted)).isLessThanOrEqualTo(60 * OnePagerTextFitterImpl.EXACT_FONT_SAFETY_MARGIN);
		assertThat(fitter.hardTruncate("W", font, 1)).isEqualTo("W");
		assertThat(fitter.hardTruncate("", font, 1)).isEmpty();
	}

	@Test
	void shouldResolveTheBundledFontOnlyForItsFamilyTest() {
		// Given: a fitter
		OnePagerTextFitterImpl fitter = new OnePagerTextFitterImpl();

		// When-Then: the bundled family is exact, anything else (or null) uses the bundled substitute
		assertThat(fitter.bundledFont(TITLE_FONT)).isPresent();
		assertThat(fitter.bundledFont("Inter Light")).isEmpty();
		assertThat(fitter.bundledFont(null)).isEmpty();
		assertThat(fitter.resolveFont(TITLE_FONT, 12.0).getFamily()).isEqualTo(TITLE_FONT);
		assertThat(fitter.resolveFont(TITLE_FONT, 12.0).getSize2D()).isEqualTo(12.0f);
		assertThat(fitter.loadBundledFont("/fonts/does-not-exist.ttf")).isEmpty();
		assertThat(fitter.loadBundledFont("/onepager/one-pager-master.pptx")).isEmpty();
	}

	@Test
	void shouldMeasureAnUnbundledFamilyWithTheEnlargedBundledFontOnEveryPlatformTest() {
		// Given: a fitter and a family that is not bundled (the KPI value's "Barlow Black")
		OnePagerTextFitterImpl fitter = new OnePagerTextFitterImpl();

		// When: fonts are resolved for that family and for no family at all
		Font unbundled = fitter.resolveFont("Barlow Black", 20.0);
		Font unnamed = fitter.resolveFont(null, 20.0);

		// Then: both are the bundled title font enlarged by the substitute factor, never a system font
		float expectedSize = (float) (20.0 * OnePagerTextFitterImpl.SUBSTITUTE_WIDTH_FACTOR);
		assertThat(unbundled.getFamily()).isEqualTo(TITLE_FONT);
		assertThat(unbundled.getSize2D()).isEqualTo(expectedSize);
		assertThat(unnamed.getFamily()).isEqualTo(TITLE_FONT);
		assertThat(unnamed.getSize2D()).isEqualTo(expectedSize);
	}

	@Test
	void shouldFallBackToTheSystemSansSerifOnlyWhenTheBundledFontCannotBeLoadedTest() {
		// Given: a fitter whose bundled title font fails to load
		OnePagerTextFitterImpl fitter = spy(new OnePagerTextFitterImpl());
		doReturn(Optional.empty()).when(fitter).loadBundledFont(OnePagerTextFitterImpl.TITLE_FONT_RESOURCE);

		// When: a family that is not bundled is resolved
		Font font = fitter.resolveFont("Barlow Black", 20.0);

		// Then: measuring degrades to the platform sans-serif at the nominal size
		assertThat(font.getFamily()).isEqualTo(Font.SANS_SERIF);
		assertThat(font.getSize2D()).isEqualTo(20.0f);
	}
}
