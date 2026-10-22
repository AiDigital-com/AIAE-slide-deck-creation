package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconSlot;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTemplateRegistryImpl;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the One-Page Case Study template registry.
 *
 * <p>The token and slot sets are the ones typed into the app's copy of the template; a key that
 * drifts here leaves a raw {@code {{token}}} on the slide.
 */
class OnePageTemplateRegistryImplTest {

	/** Every token typed into the app's copy of the template (read back from the copy). */
	private static final Set<String> TEMPLATE_TOKENS = Set.of(
			"title_line_1", "title_line_2", "description", "main_topic", "challenge", "client", "channel",
			"flight", "goal", "tactic_1_title", "tactic_1_text", "tactic_2_title", "tactic_2_text",
			"tactic_3_title", "tactic_3_text", "tactic_4_title", "tactic_4_text", "headline_lead",
			"headline_stat", "results_label", "results_text", "plan_1_title", "plan_1_text", "plan_2_title",
			"plan_2_text", "plan_3_title", "plan_3_text", "plan_4_title", "plan_4_text", "drivers_label",
			"driver_1", "driver_2", "driver_3", "driver_4", "driver_5", "stat_1_value", "stat_1_label",
			"stat_2_value", "stat_2_label", "stat_3_value", "stat_3_label");

	/** Every icon alt text tagged in the app's copy of the template. */
	private static final Set<String> TEMPLATE_ICON_SLOTS = Set.of(
			"headline_icon_1", "headline_icon_2", "tactic_1_icon", "tactic_2_icon", "tactic_3_icon",
			"tactic_4_icon", "plan_icon", "driver_1_icon", "driver_2_icon", "driver_3_icon", "driver_4_icon",
			"driver_5_icon", "stat_1_icon", "stat_2_icon");

	@Test
	void shouldMatchEveryTokenAndIconSlotInTheTemplateTest() {
		// Given: the registry built from the template facts
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();

		// When: its keys and slots are collected
		Set<String> keys = new HashSet<>();
		registry.fields().forEach(spec -> assertThat(keys.add(spec.key())).as(spec.key()).isTrue());
		Set<String> slots = new HashSet<>();
		registry.iconSlots().forEach(slot -> slots.add(slot.slot()));

		// Then: they are exactly the template's, and every slot points at known fields
		assertThat(keys).isEqualTo(TEMPLATE_TOKENS);
		assertThat(slots).isEqualTo(TEMPLATE_ICON_SLOTS);
		for (OnePageIconSlot slot : registry.iconSlots()) {
			assertThat(TEMPLATE_TOKENS).containsAll(slot.fieldKeys());
		}
	}

	@Test
	void shouldGiveEveryVisibleFieldAMeasuredBoxAndShareStatLinesTest() {
		// Given: the registry
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();

		// When-Then: stat values are character-limited, everything else has a box whose limit is
		// at least its fill target, and stat labels are measured after their value
		for (OnePageFieldSpec spec : registry.fields()) {
			if (spec.key().endsWith("_value")) {
				assertThat(spec.isMeasured()).as(spec.key()).isFalse();
			} else {
				assertThat(spec.isMeasured()).as(spec.key()).isTrue();
				assertThat(spec.box().maxLines()).as(spec.key()).isGreaterThanOrEqualTo(spec.box().minLines());
				assertThat(spec.maxChars()).as(spec.key()).isGreaterThanOrEqualTo(600);
			}
		}
		assertThat(registry.field("stat_2_label").orElseThrow().box().prefixKey()).isEqualTo("stat_2_value");
		assertThat(registry.field("not_a_field")).isEmpty();
	}

	@Test
	void shouldApplyCapitalsAndCutAtAWordBoundaryTest() {
		// Given: an all-caps title field and a character-limited stat value
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();
		OnePageFieldSpec title = registry.field("tactic_1_title").orElseThrow();
		OnePageFieldSpec statValue = registry.field("stat_1_value").orElseThrow();

		// When: raw values are formatted
		String formattedTitle = registry.formatValue(title, "  creative   sequencing ");
		String formattedStat = registry.formatValue(statValue, "-68% overall, really");

		// Then: whitespace collapses, capitals apply, and the cut lands on a word boundary
		assertThat(formattedTitle).isEqualTo("CREATIVE SEQUENCING");
		assertThat(formattedStat.length()).isLessThanOrEqualTo(statValue.maxChars());
		assertThat(formattedStat).isEqualTo("-68%");
		assertThat(registry.formatValue(title, null)).isEmpty();
		assertThat(registry.formatValue(title, "   ")).isEmpty();
	}

	@Test
	void shouldNeverLeaveDanglingPunctuationAfterACutTest() {
		// Given: a flight value far past its 30-character budget whose cut lands on a comma
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();
		OnePageFieldSpec flight = registry.field("flight").orElseThrow();
		String raw = "July, August, September, October, November, December 2026 and well beyond that";

		// When: it is formatted
		String formatted = registry.formatValue(flight, raw);

		// Then: the result is a prefix of the raw value with no trailing comma
		assertThat(formatted).doesNotEndWith(",");
		assertThat(raw).startsWith(formatted);
		assertThat(registry.formatValue(registry.field("stat_2_label").orElseThrow(), "cost per application"))
				.isEqualTo("COST PER APPLICATION");
	}

	@Test
	void shouldKeepAWordLongerThanTheBudgetRatherThanCutInsideItTest() {
		// Given: a single 10-character word for a 7-character stat value
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();
		OnePageFieldSpec statValue = registry.field("stat_1_value").orElseThrow();

		// When: it is formatted
		String formatted = registry.formatValue(statValue, "1234567890");

		// Then: the budget cuts it since there is no word boundary to fall back on
		assertThat(formatted).isEqualTo("1234567");
	}

	@Test
	void shouldFlagCampaignFactFieldsAsFactsOnlyTest() {
		// Given: the registry
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();

		// When-Then: plain facts are never lengthened with interpretation; prose fields may be
		assertThat(registry.isFactsOnly("client")).isTrue();
		assertThat(registry.isFactsOnly("stat_3_label")).isTrue();
		assertThat(registry.isFactsOnly("challenge")).isFalse();
	}
}
