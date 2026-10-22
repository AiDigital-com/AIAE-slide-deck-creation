package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthSlideSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthTemplateTokenRegistryImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InDepthTemplateTokenRegistryImpl} and the template facts it serves.
 */
class InDepthTemplateTokenRegistryImplTest {

	private final InDepthTemplateTokenRegistryImpl registry = new InDepthTemplateTokenRegistryImpl();

	@Test
	void shouldKeepEveryTokenKeyUniqueTest() {
		// Given: the token registry
		Set<String> keys = new HashSet<>();

		// When-Then: no key appears twice, otherwise one value would overwrite another
		for (InDepthTokenSpec spec : InDepthTemplateTokens.TOKENS) {
			assertThat(keys.add(spec.key())).as("duplicate token key: " + spec.key()).isTrue();
		}
	}

	@Test
	void shouldTargetOnlySlidesThatExistInTheCatalogTest() {
		// Given: the catalog's slide numbers
		Set<Integer> catalogSlideNumbers = new HashSet<>();
		for (InDepthSlideSpec slide : registry.slides()) {
			catalogSlideNumbers.add(slide.slideNumber());
		}

		// When-Then: every token points at a real slide
		for (InDepthTokenSpec spec : InDepthTemplateTokens.TOKENS) {
			for (Integer slideNumber : spec.slideNumbers()) {
				assertThat(catalogSlideNumbers)
						.as("token " + spec.key() + " targets unknown slide " + slideNumber)
						.contains(slideNumber);
			}
		}
	}

	@Test
	void shouldNumberTheCatalogSequentiallyFromOneTest() {
		// Given: the slide catalog
		List<InDepthSlideSpec> slides = registry.slides();

		// When-Then: slide numbers match deck positions, which the deleter relies on
		assertThat(slides).hasSize(24);
		for (int i = 0; i < slides.size(); i++) {
			assertThat(slides.get(i).slideNumber()).isEqualTo(i + 1);
		}
	}

	@Test
	void shouldReturnOnlyTokensTargetingTheRequestedSlideTest() {
		// Given: slides 3 and 4 are alternate layouts of the same content
		// When: each slide's tokens are listed
		List<InDepthTokenSpec> slide3 = registry.tokensForSlide(3);

		// Then: they are non-empty, scoped to the slide, and identical for both variants
		assertThat(slide3).isNotEmpty().allMatch(spec -> spec.slideNumbers().contains(3));
		assertThat(slide3).containsExactlyElementsOf(registry.tokensForSlide(4));
		assertThat(registry.tokensForSlide(99)).isEmpty();
	}

	@Test
	void shouldDraftSharedTokensOnceWhenBothVariantsAreCheckedTest() {
		// Given: both layout variants of one topic are checked
		// When: the tokens to draft are resolved
		List<InDepthTokenSpec> bothChecked = registry.tokensForSlides(List.of(3, 4));

		// Then: the shared fields are not double-drafted
		assertThat(bothChecked).containsExactlyElementsOf(registry.tokensForSlides(List.of(3)));
	}

	@Test
	void shouldUnionTokensAcrossUnrelatedCheckedSlidesTest() {
		// Given: two unrelated slides are checked
		// When: the tokens to draft are resolved
		List<InDepthTokenSpec> combined = registry.tokensForSlides(List.of(2, 16));

		// Then: both topics are drafted and nothing else
		assertThat(combined).allMatch(spec -> spec.slideNumbers().contains(2) || spec.slideNumbers().contains(16));
		assertThat(combined).anyMatch(spec -> spec.key().equals("trend_headline"));
		assertThat(combined).anyMatch(spec -> spec.key().equals("hi_headline"));
	}

	@Test
	void shouldReturnNoTokensForAnEmptyOrNullSelectionTest() {
		// Given-When-Then: nothing checked means nothing to draft
		assertThat(registry.tokensForSlides(List.of())).isEmpty();
		assertThat(registry.tokensForSlides(null)).isEmpty();
	}

	@Test
	void shouldOnlyPutPurchaseDriverStatsOnTheFullLayoutsTest() {
		// Given-When-Then: the lite layout (slide 12) has no driver stats, the full one (10) does
		assertThat(registry.tokensForSlide(12)).noneMatch(spec -> spec.key().equals("purchase_driver_1_value"));
		assertThat(registry.tokensForSlide(10)).anyMatch(spec -> spec.key().equals("purchase_driver_1_value"));
	}

	@Test
	void shouldGiveTheHighlightsSlideFourHighlightImplicationRowsTest() {
		// Given: slide 16's tokens
		List<String> keys = registry.tokensForSlide(16).stream().map(InDepthTokenSpec::key).toList();

		// When-Then: four paired rows, no fifth
		for (int row = 1; row <= 4; row++) {
			assertThat(keys).contains("hi_highlight_" + row + "_title", "hi_highlight_" + row + "_text",
					"hi_implication_" + row + "_title", "hi_implication_" + row + "_text");
		}
		assertThat(keys).doesNotContain("hi_highlight_5_title");
	}

	@Test
	void shouldScaleTheCharacterBudgetWithTheSampleTextTest() {
		// Given: a short stat value and a long intro paragraph
		InDepthTokenSpec shortStat = tokenByKey("trend_stat_1_value");
		InDepthTokenSpec longIntro = tokenByKey("trend_intro");

		// When: budgets are computed
		int shortCap = registry.maxCharsFor(shortStat);
		int longCap = registry.maxCharsFor(longIntro);

		// Then: the short field keeps an absolute buffer and the long one gets proportional room
		assertThat(shortCap).isEqualTo(shortStat.sampleText().length()
				+ InDepthTemplateTokenRegistryImpl.SAMPLE_HEADROOM_CHARS);
		assertThat(longCap).isGreaterThan(shortCap);
	}

	@Test
	void shouldUseFixedBudgetsForShortLabelBoxesAndStageTitlesTest() {
		// Given-When-Then: "CTV" would otherwise be allowed 23 characters, "ACT" 23 too
		assertThat(registry.maxCharsFor(tokenByKey("channel_1_name"))).isEqualTo(12);
		assertThat(registry.maxCharsFor(tokenByKey("donut_2_label"))).isEqualTo(24);
		assertThat(registry.maxCharsFor(tokenByKey("journey_act_title")))
				.isEqualTo(InDepthTemplateTokenRegistryImpl.JOURNEY_STAGE_TITLE_MAX_CHARS);
	}

	@Test
	void shouldListPlaceholderOnlyKeysForTheFullyTokenizedSlidesTest() {
		// Given-When-Then: collision-prone literals never fall back to sample matching
		assertThat(InDepthTemplateTokens.PLACEHOLDER_ONLY_KEYS)
				.contains("journey_act_title", "channel_1_name", "donut_3_label", "stat_chart_bar_7_value",
						"feed_stage_5_label", "hi_implication_4_text")
				.doesNotContain("trend_headline", "market_stat_1_label");
	}

	private InDepthTokenSpec tokenByKey(String key) {
		return InDepthTemplateTokens.TOKENS.stream().filter(t -> t.key().equals(key)).findFirst().orElseThrow();
	}
}
