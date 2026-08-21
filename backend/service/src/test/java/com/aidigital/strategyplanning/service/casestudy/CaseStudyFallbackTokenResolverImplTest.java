package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.impl.CaseStudyFallbackTokenResolverImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization tests for {@link CaseStudyFallbackTokenResolverImpl}.
 *
 * <p>This is the path the product takes when OpenAI is unreachable, so it decides what lands on
 * the slides in that case. The sentence split and the metric figure/label parsing are the subtle
 * parts and are pinned case by case.
 */
class CaseStudyFallbackTokenResolverImplTest {

	private CreateCaseStudyCommand command(String results, String keyMetrics) {
		return new CreateCaseStudyCommand("Acme rollout", "Acme", "Retail", "Costs were rising",
				"We rebuilt the funnel", results, keyMetrics, "Q1 2026", "Great partner",
				"brief.pdf", "user_123");
	}

	@Test
	void shouldCopyReviewedFieldsStraightIntoTheirTokensTest() {
		// Given: a reviewed case study and no AI engine
		CaseStudyFallbackTokenResolverImpl service = new CaseStudyFallbackTokenResolverImpl();

		// When: the fallback mapping runs
		Map<String, String> values = service.resolve(
				command("We cut spend.", "40% cost reduction"));

		// Then: vertical falls back to industry, and challenge/solution are copied verbatim
		assertThat(values).containsEntry("client_vertical", "Retail")
				.containsEntry("client_challenge", "Costs were rising")
				.containsEntry("solution_body", "We rebuilt the funnel");
	}

	@Test
	void shouldFallBackFromIndustryToClientThenTitleForTheVerticalTest() {
		// Given: commands missing progressively more identifying fields
		CaseStudyFallbackTokenResolverImpl service = new CaseStudyFallbackTokenResolverImpl();
		CreateCaseStudyCommand noIndustry = new CreateCaseStudyCommand("A title", "Acme", null,
				null, null, null, null, null, null, null, "user_123");
		CreateCaseStudyCommand titleOnly = new CreateCaseStudyCommand("A title", null, null,
				null, null, null, null, null, null, null, "user_123");

		// When-Then: the vertical walks industry -> client -> title
		assertThat(service.resolve(noIndustry)).containsEntry("client_vertical", "Acme");
		assertThat(service.resolve(titleOnly)).containsEntry("client_vertical", "A title");
	}

	@Test
	void shouldSplitResultsIntoAnIntroPlusUpToThreeDetailsTest() {
		// Given: four result sentences
		CaseStudyFallbackTokenResolverImpl service = new CaseStudyFallbackTokenResolverImpl();
		String results = "Spend fell 40%. Pipeline doubled. Team shipped faster. A fourth point.";

		// When: the fallback mapping runs
		Map<String, String> values = service.resolve(command(results, ""));

		// Then: the first sentence introduces and the next three become details, in order
		assertThat(values).containsEntry("results_intro", "Spend fell 40%.")
				.containsEntry("result_detail_1", "Pipeline doubled.")
				.containsEntry("result_detail_2", "Team shipped faster.")
				.containsEntry("result_detail_3", "A fourth point.");
	}

	@Test
	void shouldLeaveResultDetailsEmptyWhenResultsAreOneSentenceTest() {
		// Given: a single result sentence
		CaseStudyFallbackTokenResolverImpl service = new CaseStudyFallbackTokenResolverImpl();

		// When: the fallback mapping runs
		Map<String, String> values = service.resolve(command("Spend fell.", ""));

		// Then: only the intro is filled; the detail tokens stay blank
		assertThat(values).containsEntry("results_intro", "Spend fell.")
				.containsEntry("result_detail_1", "")
				.containsEntry("result_detail_2", "")
				.containsEntry("result_detail_3", "");
	}

	@Test
	void shouldSplitMetricsIntoFigureAndLabelPairsTest() {
		// Given: three comma-separated metrics in different figure styles
		CaseStudyFallbackTokenResolverImpl service = new CaseStudyFallbackTokenResolverImpl();
		String metrics = "40% cost reduction, 2x pipeline, $1.2M revenue";

		// When: the fallback mapping runs
		Map<String, String> values = service.resolve(command("", metrics));

		// Then: each figure and its label land in the matching token pair
		assertThat(values).containsEntry("metric_1_value", "40%")
				.containsEntry("metric_1_label", "cost reduction")
				.containsEntry("metric_2_value", "2x")
				.containsEntry("metric_2_label", "pipeline")
				.containsEntry("metric_3_value", "$1.2M")
				.containsEntry("metric_3_label", "revenue");
	}

	@Test
	void shouldKeepAnUnlabelledOrNonNumericMetricWholeTest() {
		// Given: a bare figure and a metric with no figure at all
		CaseStudyFallbackTokenResolverImpl service = new CaseStudyFallbackTokenResolverImpl();

		// When: the fallback mapping runs
		Map<String, String> values = service.resolve(
				command("", "40%, strong brand lift"));

		// Then: both keep their whole text as the value and contribute no label
		assertThat(values).containsEntry("metric_1_value", "40%")
				.containsEntry("metric_1_label", "")
				.containsEntry("metric_2_value", "strong brand lift")
				.containsEntry("metric_2_label", "");
	}

	@Test
	void shouldIgnoreBlankMetricEntriesAndStopAtThreeTest() {
		// Given: a blank entry and more metrics than the template has slots
		CaseStudyFallbackTokenResolverImpl service = new CaseStudyFallbackTokenResolverImpl();

		// When: the fallback mapping runs
		Map<String, String> values = service.resolve(
				command("", " , 10% one, 20% two, 30% three, 40% four"));

		// Then: the blank slot stays empty and the fourth metric is dropped
		assertThat(values).containsEntry("metric_1_value", "")
				.containsEntry("metric_2_value", "10%")
				.containsEntry("metric_3_value", "20%");
		assertThat(values.values()).noneMatch(value -> value.contains("40%"));
	}
}
