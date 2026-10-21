package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageFieldMeasurerImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTemplateRegistryImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTextFitterImpl;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the One-Page field measurer, wired to the real registry and fitter so the
 * verdicts are the ones the review form will show.
 */
class OnePageFieldMeasurerImplTest {

	private static final String SHORT_CHALLENGE =
			"In May, CPA soared to $564.14 due to generic ads and unfocused spend.";
	private static final String HEADLINE = "DRIVING 67% CPA REDUCTION WHILE SCALING FULL-FUNNEL YOUTUBE";

	private OnePageFieldMeasurerImpl measurer() {
		return new OnePageFieldMeasurerImpl(new OnePageTemplateRegistryImpl(), new OnePageTextFitterImpl());
	}

	private OnePageCaseStudyField field(List<OnePageCaseStudyField> fields, String key) {
		return fields.stream().filter(f -> f.key().equals(key)).findFirst().orElseThrow();
	}

	@Test
	void shouldFlagShortValuesWithAReviewerNoteAndLeaveFittingOnesCleanTest() {
		// Given: a challenge that fills two of its five lines and a headline that fits
		Map<String, String> values = new HashMap<>();
		values.put("challenge", SHORT_CHALLENGE);
		values.put("headline_stat", HEADLINE);

		// When: every field is measured
		List<OnePageCaseStudyField> fields = measurer().measureFields(values);

		// Then: the short one carries the note, the fitting one none, the blank one is EMPTY
		OnePageCaseStudyField challenge = field(fields, "challenge");
		assertThat(challenge.fit()).isEqualTo(OnePageFit.SHORT);
		assertThat(challenge.note()).contains("fills 2 of 5 lines");
		assertThat(challenge.lines()).isEqualTo(2);
		assertThat(challenge.minLines()).isEqualTo(5);
		assertThat(challenge.maxLines()).isEqualTo(6);
		OnePageCaseStudyField headline = field(fields, "headline_stat");
		assertThat(headline.fit()).isEqualTo(OnePageFit.FITS);
		assertThat(headline.note()).isNull();
		assertThat(field(fields, "driver_1").fit()).isEqualTo(OnePageFit.EMPTY);
		assertThat(fields).hasSize(new OnePageTemplateRegistryImpl().fields().size());
	}

	@Test
	void shouldReportCharacterLimitedFieldsAsFitsOrEmptyWithoutLineCountsTest() {
		// Given: one stat value filled and one blank
		Map<String, String> values = Map.of("stat_1_value", "-68%");

		// When: the fields are measured
		List<OnePageCaseStudyField> fields = measurer().measureFields(values);

		// Then: stat values are not measured in lines, and their fit only says filled or blank
		OnePageCaseStudyField filled = field(fields, "stat_1_value");
		assertThat(filled.value()).isEqualTo("-68%");
		assertThat(filled.fit()).isEqualTo(OnePageFit.FITS);
		assertThat(filled.maxLines()).isZero();
		assertThat(filled.lines()).isZero();
		assertThat(filled.note()).isNull();
		assertThat(field(fields, "stat_2_value").fit()).isEqualTo(OnePageFit.EMPTY);
	}

	@Test
	void shouldListOnlyShortAndOverValuesAsMisfitsTest() {
		// Given: a short challenge, a short headline, a fitting driver, and a blank rest
		Map<String, String> values = new HashMap<>();
		values.put("challenge", SHORT_CHALLENGE);
		values.put("headline_stat", "DRIVING 67%");
		values.put("driver_1", "Urgent promos beat generic ads");

		// When: misfits are collected
		List<OnePageFieldSpec> misfits = measurer().misfits(values);

		// Then: only the two that do not fit come back, in slide order
		assertThat(misfits).extracting(OnePageFieldSpec::key).containsExactly("challenge", "headline_stat");
	}

	@Test
	void shouldMeasureDistanceInLinesWithBlankValuesFurthestAwayTest() {
		// Given: the challenge field and candidates of every fit
		OnePageFieldMeasurerImpl measurer = measurer();
		OnePageFieldSpec challenge = new OnePageTemplateRegistryImpl().field("challenge").orElseThrow();
		Map<String, String> values = Map.of();
		String overflow = "In May, acquisition costs hit a peak of $564.14, 2.5x April's rate, driven by generic "
				+ "creative and scattered ad spend. The challenge was clear: restructure campaigns, align creative "
				+ "with key promotional moments, and hone geographic targeting to drive down costs without "
				+ "sacrificing conversion volume. Every market needed a fresh plan before peak season arrived.";

		// When-Then: fitting is 0, short counts the missing lines, over the extra, blank is maximal
		assertThat(measurer.distance(challenge, SHORT_CHALLENGE, values)).isEqualTo(3);
		assertThat(measurer.distance(challenge, overflow, values)).isBetween(1, 3);
		assertThat(measurer.distance(challenge, "", values)).isEqualTo(Integer.MAX_VALUE);
		assertThat(measurer.distance(challenge, "In May, acquisition costs hit a peak of $564.14, 2.5x April's "
				+ "rate, driven by generic creative and scattered ad spend. The challenge was clear: restructure "
				+ "campaigns, align creative with key promotional moments, and hone geographic targeting to drive "
				+ "down costs without sacrificing conversion volume.", values)).isZero();
	}

	@Test
	void shouldLookUpTheSharedLinePrefixForStatLabelsOnlyTest() {
		// Given: a stat value and label, and a field with no prefix
		OnePageFieldMeasurerImpl measurer = measurer();
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();
		Map<String, String> values = Map.of("stat_1_value", "-68%", "stat_1_label", "DEM-GEN CPA");

		// When-Then: the label is measured after its value; the challenge has no prefix
		assertThat(measurer.prefix(registry.field("stat_1_label").orElseThrow(), values)).isEqualTo("-68%");
		assertThat(measurer.prefix(registry.field("challenge").orElseThrow(), values)).isNull();
		assertThat(measurer.measure(registry.field("stat_1_label").orElseThrow(), values).lines()).isEqualTo(1);
	}
}
