package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudySummary;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconChoice;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageCaseStudyContentAssemblerImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageIconLibraryServiceImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTemplateRegistryImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for turning reviewed One-Page content into tokens, icon URLs, and the saved summary.
 */
class OnePageCaseStudyContentAssemblerImplTest {

	private OnePageCaseStudyContentAssemblerImpl assembler(String publicAssetBaseUrl) {
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setPublicAssetBaseUrl(publicAssetBaseUrl);
		return new OnePageCaseStudyContentAssemblerImpl(properties, new OnePageTemplateRegistryImpl(),
				new OnePageIconLibraryServiceImpl());
	}

	@Test
	void shouldWriteAValueForEveryTokenWithTheTemplatesCasingTest() {
		// Given: two reviewed fields, one unknown key, and the rest left out
		List<OnePageFieldValue> fields = List.of(new OnePageFieldValue("title_line_1", " scaling  conversions"),
				new OnePageFieldValue("challenge", "CPA rose."), new OnePageFieldValue("mystery", "ignored"));

		// When: token values are built
		Map<String, String> tokens = assembler("").buildTokenValues(fields);

		// Then: every template token has a value, formatted, and nothing else leaks in
		assertThat(tokens).hasSize(new OnePageTemplateRegistryImpl().fields().size());
		assertThat(tokens).containsEntry("title_line_1", "SCALING CONVERSIONS");
		assertThat(tokens).containsEntry("challenge", "CPA rose.");
		assertThat(tokens).containsEntry("driver_5", "");
		assertThat(tokens).doesNotContainKey("mystery");
		assertThat(assembler("").buildTokenValues(null)).containsEntry("challenge", "");
	}

	@Test
	void shouldPreferTheConfiguredPublicBaseUrlOverTheRequestsTest() {
		// Given: a configured public asset base and a localhost request
		List<OnePageIconChoice> icons = List.of(new OnePageIconChoice("tactic_1_icon", "target"),
				new OnePageIconChoice("tactic_2_icon", null), new OnePageIconChoice("plan_icon", "not-an-icon"));

		// When: icon URLs are built
		Map<String, String> urls = assembler("https://app.example.com/").buildIconUrls(icons, "http://localhost:5000");

		// Then: only the known icon is mapped, on the configured host
		assertThat(urls).containsExactly(Map.entry("tactic_1_icon", "https://app.example.com/case-study-icons/target.png"));
	}

	@Test
	void shouldFallBackToTheRequestBaseUrlAndSkipWithoutAnyBaseTest() {
		// Given: no configured public base
		List<OnePageIconChoice> icons = List.of(new OnePageIconChoice("driver_1_icon", "brain"));

		// When-Then: the request's own address serves the icons; with no address at all, no swap
		assertThat(assembler("").buildIconUrls(icons, "https://deployed.example.com"))
				.containsEntry("driver_1_icon", "https://deployed.example.com/case-study-icons/brain.png");
		assertThat(assembler("").buildIconUrls(icons, "")).isEmpty();
		assertThat(assembler("").buildIconUrls(null, "https://deployed.example.com")).isEmpty();
	}

	@Test
	void shouldSummariseTheTokensForTheSavedRowTest() {
		// Given: filled tokens with two of four tactics and two of three stats
		OnePageCaseStudyContentAssemblerImpl assembler = assembler("");
		Map<String, String> tokens = assembler.buildTokenValues(List.of(
				new OnePageFieldValue("title_line_1", "Scaling conversions"),
				new OnePageFieldValue("title_line_2", "while cutting CPA"),
				new OnePageFieldValue("client", "A home improvement company"),
				new OnePageFieldValue("main_topic", "home services"),
				new OnePageFieldValue("challenge", "CPA peaked in May."),
				new OnePageFieldValue("tactic_1_text", "Reallocated budget."),
				new OnePageFieldValue("tactic_3_text", "Matched ad length."),
				new OnePageFieldValue("results_text", "Conversions surpassed April."),
				new OnePageFieldValue("headline_stat", "Driving 67% CPA reduction"),
				new OnePageFieldValue("stat_1_value", "-68%"),
				new OnePageFieldValue("stat_3_value", "-67%"),
				new OnePageFieldValue("flight", "April – August 2026")));

		// When: the summary is derived
		OnePageCaseStudySummary summary = assembler.summarize(tokens);

		// Then: the row reads like a three-page case study, with blanks as null
		assertThat(summary.title()).isEqualTo("SCALING CONVERSIONS WHILE CUTTING CPA");
		assertThat(summary.clientName()).isEqualTo("A home improvement company");
		assertThat(summary.industry()).isEqualTo("HOME SERVICES");
		assertThat(summary.challenge()).isEqualTo("CPA peaked in May.");
		assertThat(summary.solution()).isEqualTo("Reallocated budget., Matched ad length.");
		assertThat(summary.results()).isEqualTo("Conversions surpassed April.");
		assertThat(summary.keyMetrics()).isEqualTo("DRIVING 67% CPA REDUCTION, -68%, -67%");
		assertThat(summary.timeline()).isEqualTo("April – August 2026");
	}

	@Test
	void shouldUseADefaultTitleAndNullsWhenNothingWasFilledTest() {
		// Given: no reviewed content
		OnePageCaseStudyContentAssemblerImpl assembler = assembler("");

		// When: the summary is derived from blank tokens
		OnePageCaseStudySummary summary = assembler.summarize(assembler.buildTokenValues(List.of()));

		// Then: the row still has a title and no empty-string columns
		assertThat(summary.title()).isEqualTo(OnePageCaseStudyContentAssemblerImpl.DEFAULT_TITLE);
		assertThat(summary.clientName()).isNull();
		assertThat(summary.solution()).isNull();
		assertThat(summary.keyMetrics()).isNull();
	}
}
