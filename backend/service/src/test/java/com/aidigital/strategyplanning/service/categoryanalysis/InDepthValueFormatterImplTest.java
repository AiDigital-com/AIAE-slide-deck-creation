package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthTemplateTokenRegistryImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthValueFormatterImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InDepthValueFormatterImpl}.
 */
class InDepthValueFormatterImplTest {

	private final InDepthTemplateTokenRegistryImpl registry = new InDepthTemplateTokenRegistryImpl();
	private final InDepthValueFormatterImpl formatter = new InDepthValueFormatterImpl(registry);

	@Test
	void shouldUpperCaseAllCapsFieldsTest() {
		// Given-When-Then: titles the master styles in caps are upper-cased
		assertThat(formatter.formatForSlide("hi_highlight_1_title", "Demand Anchors the Category"))
				.isEqualTo("DEMAND ANCHORS THE CATEGORY");
		assertThat(formatter.formatForSlide("feed_stage_2_label", "consideration")).isEqualTo("CONSIDERATION");
	}

	@Test
	void shouldEndMarketStatLabelsWithAPeriodTest() {
		// Given-When-Then: a closing period is added once
		assertThat(formatter.formatForSlide("market_stat_1_label", "New stores planned in FY2026"))
				.isEqualTo("New stores planned in FY2026.");
		assertThat(formatter.formatForSlide("market_stat_1_label", "Already ends.")).isEqualTo("Already ends.");
	}

	@Test
	void shouldLeaveOtherFieldsAndBlankValuesUnchangedTest() {
		// Given-When-Then: no rule applies
		assertThat(formatter.formatForSlide("hi_highlight_1_text", " Mixed Case text ")).isEqualTo("Mixed Case text");
		assertThat(formatter.formatForSlide("hi_highlight_1_text", null)).isNull();
		assertThat(formatter.formatForSlide("hi_highlight_1_text", "  ")).isEqualTo("  ");
	}

	@Test
	void shouldDropThePercentSignTheMasterAlreadyPrintsTest() {
		// Given-When-Then: only the keys whose box prints "%" lose it
		assertThat(formatter.formatForDeck("stat_1_value", "95%")).isEqualTo("95");
		assertThat(formatter.formatForDeck("stat_chart_bar_3_value", "30 %")).isEqualTo("30");
		assertThat(formatter.formatForDeck("trend_stat_2_value", "54%")).isEqualTo("54%");
		assertThat(formatter.formatForDeck("stat_1_value", null)).isNull();
	}

	@Test
	void shouldLeaveValuesWithinBudgetAloneTest() {
		// Given: a short stat value
		InDepthTokenSpec spec = tokenByKey("trend_stat_1_value");

		// When-Then: nothing is cut
		assertThat(formatter.capToFit(spec, "11.9M")).isEqualTo("11.9M");
	}

	@Test
	void shouldTruncateOverLongValuesAtAWordBoundaryTest() {
		// Given: a value far past the budget
		InDepthTokenSpec spec = tokenByKey("trend_stat_1_value");
		int max = registry.maxCharsFor(spec);
		String tooLong = "a".repeat(max - 5) + " and then a great deal more unnecessary text after that";

		// When: it is capped
		String result = formatter.capToFit(spec, tooLong);

		// Then: it fits and the cut lands on a word boundary
		assertThat(result.length()).isLessThanOrEqualTo(max);
		assertThat(result).isEqualTo("a".repeat(max - 5) + " and");
	}

	@Test
	void shouldCutMidWordWhenNoSpaceIsLateEnoughTest() {
		// Given: a single unbroken word past the budget
		InDepthTokenSpec spec = tokenByKey("trend_stat_1_value");
		int max = registry.maxCharsFor(spec);

		// When-Then: the value is hard-cut at the budget
		assertThat(formatter.capToFit(spec, "x".repeat(max + 50))).hasSize(max);
	}

	@Test
	void shouldRecognisePlaceholderNonAnswersTest() {
		// Given-When-Then: blanks and every common non-answer count as missing
		for (String placeholder : Arrays.asList(null, "", "   ", "-", "--", "N/A", "n/a", "Unknown", "Not available",
				"Data not available.", "TBD", "Unable to verify", "Missing data")) {
			assertThat(formatter.isPlaceholder(placeholder)).as(placeholder).isTrue();
		}
	}

	@Test
	void shouldAcceptRealContentTest() {
		// Given-When-Then: actual values are kept
		for (String real : List.of("54%", "11.9M", "Source: KFF, Medicare Advantage in 2025",
				"MEDICARE ADVANTAGE ENROLLMENT RATE")) {
			assertThat(formatter.isPlaceholder(real)).as(real).isFalse();
		}
	}

	@Test
	void shouldReadTheLeadingNumberOfAChartValueTest() {
		// Given-When-Then: percentages, thousands separators, decimals, and non-numbers
		assertThat(formatter.chartNumber("45%")).isEqualTo(45);
		assertThat(formatter.chartNumber("1,200 stores")).isEqualTo(1200);
		assertThat(formatter.chartNumber("11.9M")).isEqualTo(11.9);
		assertThat(formatter.chartNumber("n/a")).isZero();
		assertThat(formatter.chartNumber(null)).isZero();
	}

	@Test
	void shouldPutTheLargestDonutSliceFirstAndTheSmallestBarFirstTest() {
		// Given: unsorted donut and bar series
		Map<String, String> values = new HashMap<>();
		values.put("donut_1_label", "Small");
		values.put("donut_1_value", "10%");
		values.put("donut_2_label", "Big");
		values.put("donut_2_value", "50%");
		values.put("donut_3_label", "Mid");
		values.put("donut_3_value", "25%");
		values.put("donut_4_label", "Low");
		values.put("donut_4_value", "15%");
		for (int n = 1; n <= InDepthTemplateTokens.STAT_CHART_BARS; n++) {
			values.put("stat_chart_bar_" + n + "_label", "Bar " + (8 - n));
			values.put("stat_chart_bar_" + n + "_value", (80 - 10 * n) + "%");
		}

		// When: the series are sorted
		Map<String, String> sorted = formatter.sortChartSeries(values);

		// Then: labels move with their values, in the order each chart draws them
		assertThat(List.of(sorted.get("donut_1_label"), sorted.get("donut_2_label"),
				sorted.get("donut_3_label"), sorted.get("donut_4_label")))
				.containsExactly("Big", "Mid", "Low", "Small");
		assertThat(sorted.get("donut_1_value")).isEqualTo("50%");
		assertThat(sorted.get("stat_chart_bar_1_value")).isEqualTo("10%");
		assertThat(sorted.get("stat_chart_bar_1_label")).isEqualTo("Bar 1");
		assertThat(sorted.get("stat_chart_bar_7_value")).isEqualTo("70%");
	}

	@Test
	void shouldLeaveAnIncompleteSeriesAloneTest() {
		// Given: only one slice of the donut is present
		Map<String, String> values = Map.of("donut_1_label", "A", "donut_1_value", "5%");

		// When-Then: nothing is reordered
		assertThat(formatter.sortChartSeries(values)).isEqualTo(values);
	}

	private InDepthTokenSpec tokenByKey(String key) {
		return InDepthTemplateTokens.TOKENS.stream().filter(t -> t.key().equals(key)).findFirst().orElseThrow();
	}
}
