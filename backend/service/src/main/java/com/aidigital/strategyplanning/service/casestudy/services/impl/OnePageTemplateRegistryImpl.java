package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSource;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconSlot;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextBox;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTemplateRegistry;
import com.aidigital.strategyplanning.service.casestudy.templates.OnePageCaseStudyTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link OnePageTemplateRegistry}. Assembles the field and icon-slot
 * lists once from the facts in {@link OnePageCaseStudyTemplate}.
 */
@Service
public class OnePageTemplateRegistryImpl implements OnePageTemplateRegistry {

	private static final String WHITESPACE_RUN = "\\s+";
	private static final String SINGLE_SPACE = " ";
	private static final String TRAILING_PUNCTUATION = "[\\s,;:\\-–—]+$";

	private final List<OnePageFieldSpec> fields;
	private final Map<String, OnePageFieldSpec> fieldsByKey;
	private final List<OnePageIconSlot> iconSlots;

	/**
	 * Builds the registry from the template facts.
	 */
	public OnePageTemplateRegistryImpl() {
		this.fields = List.copyOf(buildFields());
		Map<String, OnePageFieldSpec> byKey = new LinkedHashMap<>();
		for (OnePageFieldSpec spec : fields) {
			byKey.put(spec.key(), spec);
		}
		this.fieldsByKey = Map.copyOf(byKey);
		this.iconSlots = List.copyOf(buildIconSlots());
	}

	@Override
	public List<OnePageFieldSpec> fields() {
		return fields;
	}

	@Override
	public List<OnePageIconSlot> iconSlots() {
		return iconSlots;
	}

	@Override
	public Optional<OnePageFieldSpec> field(String key) {
		return Optional.ofNullable(fieldsByKey.get(key));
	}

	@Override
	public String formatValue(OnePageFieldSpec spec, String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String formatted = value.trim().replaceAll(WHITESPACE_RUN, SINGLE_SPACE);
		if (spec.allCaps()) {
			formatted = formatted.toUpperCase(Locale.ROOT);
		}
		if (formatted.length() > spec.maxChars()) {
			String cut = formatted.substring(0, spec.maxChars());
			int lastSpace = cut.lastIndexOf(' ');
			// Never cut inside a word, and never leave dangling punctuation.
			formatted = (lastSpace > 0 ? cut.substring(0, lastSpace) : cut).replaceAll(TRAILING_PUNCTUATION, "");
		}
		return formatted;
	}

	@Override
	public boolean isFactsOnly(String key) {
		return OnePageCaseStudyTemplate.FACTS_ONLY_KEYS.contains(key);
	}

	/**
	 * Builds the field list in slide order.
	 *
	 * @return every template text field
	 */
	List<OnePageFieldSpec> buildFields() {
		List<OnePageFieldSpec> result = new ArrayList<>();
		result.add(spec(OnePageCaseStudyTemplate.KEY_TITLE_LINE_1, "Title — line 1",
				OnePageCaseStudyTemplate.SECTION_TOP, OnePageFieldSource.WRITTEN, 24, true, "SCALING CONVERSIONS"));
		result.add(spec(OnePageCaseStudyTemplate.KEY_TITLE_LINE_2, "Title — line 2",
				OnePageCaseStudyTemplate.SECTION_TOP, OnePageFieldSource.WRITTEN, 24, true, "WHILE CUTTING CPA"));
		result.add(spec(OnePageCaseStudyTemplate.KEY_DESCRIPTION, "Short description",
				OnePageCaseStudyTemplate.SECTION_TOP, OnePageFieldSource.WRITTEN, 140, false,
				"How a home services brand restructured YouTube to cut acquisition costs by two-thirds."));
		result.add(spec(OnePageCaseStudyTemplate.KEY_MAIN_TOPIC, "Main topic (industry)",
				OnePageCaseStudyTemplate.SECTION_TOP, OnePageFieldSource.WRITTEN, 22, true, "HOME SERVICES"));
		result.add(spec(OnePageCaseStudyTemplate.KEY_CHALLENGE, "The challenge",
				OnePageCaseStudyTemplate.SECTION_FACTS, OnePageFieldSource.FILES, 380, false,
				"In May, acquisition costs hit a peak of $564.14, 2.5x April's rate, driven by generic "
						+ "creative and scattered ad spend."));
		result.add(spec(OnePageCaseStudyTemplate.KEY_CLIENT, "Client (generic description)",
				OnePageCaseStudyTemplate.SECTION_FACTS, OnePageFieldSource.FILES, 70, false,
				"A home improvement company covering windows, roofing, and baths"));
		result.add(spec(OnePageCaseStudyTemplate.KEY_CHANNEL, "Channel",
				OnePageCaseStudyTemplate.SECTION_FACTS, OnePageFieldSource.FILES, 50, false,
				"YouTube - Skippable In-Stream and Demand Gen"));
		result.add(spec(OnePageCaseStudyTemplate.KEY_FLIGHT, "Flight",
				OnePageCaseStudyTemplate.SECTION_FACTS, OnePageFieldSource.FILES, 30, false, "April – August 2026"));
		result.add(spec(OnePageCaseStudyTemplate.KEY_GOAL, "Goal",
				OnePageCaseStudyTemplate.SECTION_FACTS, OnePageFieldSource.FILES, 75, false,
				"Scale conversion volume while sharply reducing cost per acquisition."));
		for (int n = 1; n <= OnePageCaseStudyTemplate.TACTIC_COUNT; n++) {
			String[] example = OnePageCaseStudyTemplate.TACTIC_EXAMPLES[n - 1];
			result.add(spec(OnePageCaseStudyTemplate.TACTIC_TITLE_KEY.formatted(n), "Tactic " + n + " — title",
					OnePageCaseStudyTemplate.SECTION_TACTICS, OnePageFieldSource.WRITTEN, 26, true, example[0]));
			result.add(spec(OnePageCaseStudyTemplate.TACTIC_TEXT_KEY.formatted(n), "Tactic " + n + " — what was done",
					OnePageCaseStudyTemplate.SECTION_TACTICS, OnePageFieldSource.FILES, 80, false, example[1]));
		}
		result.add(spec(OnePageCaseStudyTemplate.KEY_HEADLINE_LEAD, "Headline — lead-in",
				OnePageCaseStudyTemplate.SECTION_RESULTS, OnePageFieldSource.WRITTEN, 30, true,
				"HOW SMART RESTRUCTURING WON"));
		result.add(spec(OnePageCaseStudyTemplate.KEY_HEADLINE_STAT, "Headline — key result",
				OnePageCaseStudyTemplate.SECTION_RESULTS, OnePageFieldSource.FILES, 90, true,
				"DRIVING 67% CPA REDUCTION WHILE SCALING FULL-FUNNEL YOUTUBE"));
		result.add(spec(OnePageCaseStudyTemplate.KEY_RESULTS_LABEL, "Results label",
				OnePageCaseStudyTemplate.SECTION_RESULTS, OnePageFieldSource.WRITTEN, 24, true, "PROVEN CPA RESULTS"));
		result.add(spec(OnePageCaseStudyTemplate.KEY_RESULTS_TEXT, "Results summary",
				OnePageCaseStudyTemplate.SECTION_RESULTS, OnePageFieldSource.FILES, 100, false,
				"Driven by Demand Gen, August conversions surpassed April's baseline at one-third of May's CPA."));
		for (int n = 1; n <= OnePageCaseStudyTemplate.PLAN_STEP_COUNT; n++) {
			String[] example = OnePageCaseStudyTemplate.PLAN_EXAMPLES[n - 1];
			result.add(spec(OnePageCaseStudyTemplate.PLAN_TITLE_KEY.formatted(n), "Step " + n + " — title",
					OnePageCaseStudyTemplate.SECTION_PLAN, OnePageFieldSource.WRITTEN, 20, true, example[0]));
			result.add(spec(OnePageCaseStudyTemplate.PLAN_TEXT_KEY.formatted(n), "Step " + n + " — description",
					OnePageCaseStudyTemplate.SECTION_PLAN, OnePageFieldSource.FILES, 90, false, example[1]));
		}
		result.add(spec(OnePageCaseStudyTemplate.KEY_DRIVERS_LABEL, "Drivers label",
				OnePageCaseStudyTemplate.SECTION_DRIVERS, OnePageFieldSource.WRITTEN, 26, true,
				"DRIVERS OF EFFICIENCY"));
		for (int n = 1; n <= OnePageCaseStudyTemplate.DRIVER_COUNT; n++) {
			result.add(spec(OnePageCaseStudyTemplate.DRIVER_KEY.formatted(n), "Driver " + n,
					OnePageCaseStudyTemplate.SECTION_DRIVERS, OnePageFieldSource.FILES, 46, false,
					OnePageCaseStudyTemplate.DRIVER_EXAMPLES[n - 1]));
		}
		for (int n = 1; n <= OnePageCaseStudyTemplate.STAT_COUNT; n++) {
			String[] example = OnePageCaseStudyTemplate.STAT_EXAMPLES[n - 1];
			result.add(spec(OnePageCaseStudyTemplate.STAT_VALUE_KEY.formatted(n), "Stat " + n + " — value",
					OnePageCaseStudyTemplate.SECTION_STATS, OnePageFieldSource.FILES, 7, true, example[0]));
			result.add(spec(OnePageCaseStudyTemplate.STAT_LABEL_KEY.formatted(n), "Stat " + n + " — label",
					OnePageCaseStudyTemplate.SECTION_STATS, OnePageFieldSource.FILES, 18, true, example[1]));
		}
		return result;
	}

	/**
	 * Declares a field, attaching its measured box when it has one. Measured fields are limited by
	 * lines in their box; the character ceiling is only a generous safety net so it never cuts
	 * text the box could hold.
	 *
	 * @param key      token key
	 * @param label    human-readable label
	 * @param section  template section
	 * @param source   where the content may come from
	 * @param maxChars hard character ceiling
	 * @param allCaps  whether the template uses capitals
	 * @param example  example value
	 * @return the field spec
	 */
	OnePageFieldSpec spec(String key, String label, String section, OnePageFieldSource source, int maxChars,
			boolean allCaps, String example) {
		OnePageTextBox box = OnePageCaseStudyTemplate.BOXES.get(key);
		int ceiling = box == null ? maxChars : Math.max(maxChars, OnePageCaseStudyTemplate.BOX_SAFETY_CHARS);
		return new OnePageFieldSpec(key, label, section, source, ceiling, allCaps, example, box);
	}

	/**
	 * Builds the icon slot list, matching the alt text tagged in the template.
	 *
	 * @return every tagged icon slot
	 */
	List<OnePageIconSlot> buildIconSlots() {
		List<OnePageIconSlot> slots = new ArrayList<>();
		for (int n = 1; n <= OnePageCaseStudyTemplate.HEADLINE_ICON_COUNT; n++) {
			slots.add(new OnePageIconSlot(OnePageCaseStudyTemplate.HEADLINE_ICON_SLOT.formatted(n),
					"Headline icon " + n,
					List.of(OnePageCaseStudyTemplate.KEY_HEADLINE_LEAD, OnePageCaseStudyTemplate.KEY_HEADLINE_STAT)));
		}
		for (int n = 1; n <= OnePageCaseStudyTemplate.TACTIC_COUNT; n++) {
			slots.add(new OnePageIconSlot(OnePageCaseStudyTemplate.TACTIC_ICON_SLOT.formatted(n),
					"Tactic " + n + " icon",
					List.of(OnePageCaseStudyTemplate.TACTIC_TITLE_KEY.formatted(n),
							OnePageCaseStudyTemplate.TACTIC_TEXT_KEY.formatted(n))));
		}
		List<String> planKeys = new ArrayList<>();
		for (int n = 1; n <= OnePageCaseStudyTemplate.PLAN_STEP_COUNT; n++) {
			planKeys.add(OnePageCaseStudyTemplate.PLAN_TITLE_KEY.formatted(n));
		}
		slots.add(new OnePageIconSlot(OnePageCaseStudyTemplate.PLAN_ICON_SLOT, "Game plan icon",
				List.copyOf(planKeys)));
		for (int n = 1; n <= OnePageCaseStudyTemplate.DRIVER_COUNT; n++) {
			slots.add(new OnePageIconSlot(OnePageCaseStudyTemplate.DRIVER_ICON_SLOT.formatted(n),
					"Driver " + n + " icon", List.of(OnePageCaseStudyTemplate.DRIVER_KEY.formatted(n))));
		}
		for (int n = 1; n <= OnePageCaseStudyTemplate.STAT_ICON_COUNT; n++) {
			slots.add(new OnePageIconSlot(OnePageCaseStudyTemplate.STAT_ICON_SLOT.formatted(n),
					"Stat " + n + " icon",
					List.of(OnePageCaseStudyTemplate.STAT_VALUE_KEY.formatted(n),
							OnePageCaseStudyTemplate.STAT_LABEL_KEY.formatted(n))));
		}
		return slots;
	}
}
