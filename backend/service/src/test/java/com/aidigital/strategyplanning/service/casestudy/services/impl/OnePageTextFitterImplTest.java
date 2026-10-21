package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextMeasurement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks {@link OnePageTextFitterImpl} against line breaks Google Slides actually rendered in a
 * generated One-Page Case Study deck, and against the designer's original case study, which was
 * written to fill each box.
 */
class OnePageTextFitterImplTest {

	private static final String SHORT_CHALLENGE = "In May, CPA soared to $564.14 due to generic ads and unfocused "
			+ "spend. Needed a strategic overhaul to cut costs and boost conversions.";
	private static final String FULL_CHALLENGE = "In May, acquisition costs hit a peak of $564.14, 2.5x April's "
			+ "rate, driven by generic creative and scattered ad spend. The challenge was clear: restructure "
			+ "campaigns, align creative with key promotional moments, and hone geographic targeting to drive down "
			+ "costs without sacrificing conversion volume.";
	private static final String HEADLINE = "DRIVING 67% CPA REDUCTION WHILE SCALING FULL-FUNNEL YOUTUBE";
	private static final String LONG_HEADLINE = HEADLINE + " FOR HOME SERVICES";

	private OnePageFieldSpec field(String key) {
		return new OnePageTemplateRegistryImpl().field(key).orElseThrow();
	}

	@Test
	void shouldWrapTheChallengeExactlyLikeGoogleSlidesAcrossTheWidthBandTest() {
		// Given: a challenge Google Slides rendered on three lines
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();
		List<String> rendered = List.of("In May, CPA soared to $564.14 due to generic ads and",
				"unfocused spend. Needed a strategic overhaul to cut", "costs and boost conversions.");

		// When: it is wrapped at both ends of the band
		List<String> wide = fitter.wrap(SHORT_CHALLENGE, field("challenge").box(), OnePageTextFitterImpl.WIDE);
		List<String> narrow = fitter.wrap(SHORT_CHALLENGE, field("challenge").box(), OnePageTextFitterImpl.NARROW);

		// Then: the breaks land where Google put them
		assertThat(wide).isEqualTo(rendered);
		assertThat(narrow).hasSize(3);
	}

	@Test
	void shouldEndLinesWhereGoogleSlidesDidInTheCampaignFactBoxesTest() {
		// Given: the campaign fact values from a rendered deck
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();

		// When-Then: each box breaks where the rendered deck did
		assertThat(fitter.wrap("A home improvement company covering windows, roofing, and baths",
				field("client").box(), OnePageTextFitterImpl.WIDE)).endsWith("windows, roofing, and", "baths");
		assertThat(fitter.wrap("YouTube - Skippable In-Stream and Demand Gen", field("channel").box(),
				OnePageTextFitterImpl.WIDE)).endsWith("Demand Gen");
		assertThat(fitter.wrap("Scale conversion volume while sharply reducing cost per acquisition.",
				field("goal").box(), OnePageTextFitterImpl.WIDE)).endsWith("per acquisition.");
	}

	@Test
	void shouldFindTheDesignersOriginalTextFillsItsBoxesTest() {
		// Given: the original case study's copy
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();

		// When-Then: every box the designer filled measures as FITS
		assertThat(fitter.measure(field("challenge"), FULL_CHALLENGE, null).fit()).isEqualTo(OnePageFit.FITS);
		assertThat(fitter.measure(field("headline_stat"), HEADLINE, null).fit()).isEqualTo(OnePageFit.FITS);
		for (String driver : List.of("80.23% VCR captured upper-funnel awareness", "Upper-funnel views fed conversions",
				"Urgent promos beat generic ads", "Geo-targeting halved local CPAs")) {
			assertThat(fitter.measure(field("driver_1"), driver, null).fit()).as(driver).isEqualTo(OnePageFit.FITS);
		}
	}

	@Test
	void shouldFlagShortFieldsOverflowAndEmptyBoxesTest() {
		// Given: a short challenge, a short and a long headline, and a blank
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();

		// When: they are measured
		OnePageTextMeasurement challenge = fitter.measure(field("challenge"), SHORT_CHALLENGE, null);

		// Then: each verdict matches what the slide would show
		assertThat(challenge.fit()).isEqualTo(OnePageFit.SHORT);
		assertThat(challenge.lines()).isEqualTo(3);
		assertThat(challenge.minLines()).isEqualTo(5);
		assertThat(challenge.maxLines()).isEqualTo(6);
		assertThat(fitter.measure(field("headline_stat"), "DRIVING 67%", null).fit()).isEqualTo(OnePageFit.SHORT);
		assertThat(fitter.measure(field("headline_stat"), LONG_HEADLINE, null).fit()).isEqualTo(OnePageFit.OVER);
		assertThat(fitter.measure(field("challenge"), "", null).fit()).isEqualTo(OnePageFit.EMPTY);
		assertThat(fitter.measure(field("challenge"), null, null).fit()).isEqualTo(OnePageFit.EMPTY);
	}

	@Test
	void shouldTrimWholeWordsUntilTheTextFitsTest() {
		// Given: a headline that runs past its box
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();

		// When: it is trimmed
		String trimmed = fitter.trimToFit(field("headline_stat"), LONG_HEADLINE, null);

		// Then: the result fits and is a prefix of the original
		assertThat(fitter.measure(field("headline_stat"), trimmed, null).fit()).isNotEqualTo(OnePageFit.OVER);
		assertThat(LONG_HEADLINE).startsWith(trimmed);
		assertThat(fitter.trimToFit(field("headline_stat"), HEADLINE, null)).isEqualTo(HEADLINE);
	}

	@Test
	void shouldMeasureStatLabelsTogetherWithTheirValueTest() {
		// Given: a stat label and the value that shares its line
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();

		// When-Then: the pair still sits on one line
		assertThat(fitter.measure(field("stat_1_label"), "DEM-GEN CPA", "-68%").lines()).isEqualTo(1);
		assertThat(fitter.measure(field("stat_1_label"), "DEM-GEN CPA", "  ").lines()).isEqualTo(1);
	}

	@Test
	void shouldDropWholeSentencesWhenTheBoxStillFillsTest() {
		// Given: six filling lines plus one sentence too many
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();
		String tooLong = FULL_CHALLENGE + " Every market needed a fresh plan before peak season arrived.";

		// When: it is trimmed
		String trimmed = fitter.trimToFit(field("challenge"), tooLong, null);

		// Then: only the extra sentence went
		assertThat(fitter.measure(field("challenge"), tooLong, null).fit()).isEqualTo(OnePageFit.OVER);
		assertThat(trimmed).isEqualTo(FULL_CHALLENGE);
	}

	@Test
	void shouldCloseTheSentenceInsteadOfEndingMidThoughtAsALastResortTest() {
		// Given: one long sentence (nothing to drop whole) that runs past the challenge box
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();
		String oneSentence = "In May, acquisition costs soared to a peak of $564.14, which was 2.5 times higher than "
				+ "April's rate because generic creative and scattered ad spend spread budget thin across every "
				+ "market, so the team set out to restructure campaigns, align creative with key promotional "
				+ "moments, and hone geographic targeting to drive down costs without sacrificing conversion "
				+ "volume across the windows, roofing, and bath lines during the busiest months of the season.";

		// When: it is trimmed
		String trimmed = fitter.trimToFit(field("challenge"), oneSentence, null);

		// Then: it fits, ends with a period, and does not end on a connector word
		assertThat(fitter.measure(field("challenge"), oneSentence, null).fit()).isEqualTo(OnePageFit.OVER);
		assertThat(trimmed).endsWith(".");
		assertThat(fitter.measure(field("challenge"), trimmed, null).fit()).isNotEqualTo(OnePageFit.OVER);
		String lastWord = trimmed.substring(trimmed.lastIndexOf(' ') + 1).replace(".", "");
		assertThat(List.of("to", "and", "the", "of", "with", "without")).doesNotContain(lastWord);
	}

	@Test
	void shouldLeaveTheValueUnchangedWhenWholeSentencesCannotFitTest() {
		// Given: a one-sentence headline that overflows
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();

		// When-Then: sentence trimming has nothing to drop and returns the input
		assertThat(fitter.trimSentencesToFit(field("headline_stat"), LONG_HEADLINE, null)).isEqualTo(LONG_HEADLINE);
	}

	@Test
	void shouldEstimateMoreCharactersPerLineForSmallerFontsTest() {
		// Given: a 12pt challenge box and an 8pt tactic box of similar width
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();

		// When: characters per line are estimated
		int challenge = fitter.charsPerLine(field("challenge"));
		int tactic = fitter.charsPerLine(field("tactic_1_text"));
		int title = fitter.charsPerLine(field("title_line_1"));

		// Then: the estimates are positive and ordered by how much copy each box holds
		assertThat(challenge).isGreaterThan(30);
		assertThat(tactic).isGreaterThan(30);
		assertThat(title).isGreaterThan(5).isLessThan(challenge);
	}

	@Test
	void shouldFailLoudlyWhenABundledFontIsMissingTest() {
		// Given: a font file name that is not bundled
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();

		// When-Then: the fitter refuses rather than measuring with a fallback font
		assertThatThrownBy(() -> fitter.font("Missing-Font.ttf"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Missing bundled font");
	}
}
