package com.aidigital.strategyplanning.service.casestudy.templates;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextBox;

import java.util.Map;
import java.util.Set;

/**
 * Canonical registry for the One-Page Case Study Google Slides template: every {@code {{token}}}
 * text field and every icon slot (an image whose alt text is {@code {{slot}}}).
 *
 * <p>Each field says where its content may come from. {@code FILES} fields
 * carry facts — figures, dates, channels, results — and must come only from the uploaded
 * documents. {@code WRITTEN} fields are short titles and labels the AI may
 * phrase for flow, still grounded in the documents' story. Character budgets come from the
 * template's example case study, which shows how much each box holds. The field and icon-slot
 * lists are assembled from these facts by
 * {@link com.aidigital.strategyplanning.service.casestudy.services.OnePageTemplateRegistry}, which
 * also owns lookups and formatting; this class only holds the template facts.
 */
public final class OnePageCaseStudyTemplate {

	/** Font files bundled under {@code fonts/case-study/}. */
	public static final String FONT_BARLOW_BLACK = "BarlowSemiCondensed-Black.ttf";
	public static final String FONT_BARLOW_EXTRA_BOLD = "BarlowSemiCondensed-ExtraBold.ttf";
	public static final String FONT_BARLOW_BOLD = "BarlowSemiCondensed-Bold.ttf";
	public static final String FONT_INTER_LIGHT = "Inter-Light.ttf";
	public static final String FONT_INTER_REGULAR = "Inter-Regular.ttf";
	public static final String FONT_INTER_BOLD = "Inter-Bold.ttf";
	public static final String FONT_INTER_BLACK = "Inter-Black.ttf";
	public static final String FONT_ROBOTO_REGULAR = "Roboto-Regular.ttf";

	/** Template sections, in slide order. */
	public static final String SECTION_TOP = "Title & summary";
	public static final String SECTION_FACTS = "Challenge & campaign";
	public static final String SECTION_TACTICS = "Tactics";
	public static final String SECTION_RESULTS = "Headline & results";
	public static final String SECTION_PLAN = "The game plan";
	public static final String SECTION_DRIVERS = "Drivers";
	public static final String SECTION_STATS = "Key stats";

	/** Keys of the fields that carry the deck title. */
	public static final String KEY_TITLE_LINE_1 = "title_line_1";
	public static final String KEY_TITLE_LINE_2 = "title_line_2";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_MAIN_TOPIC = "main_topic";
	public static final String KEY_CHALLENGE = "challenge";
	public static final String KEY_CLIENT = "client";
	public static final String KEY_CHANNEL = "channel";
	public static final String KEY_FLIGHT = "flight";
	public static final String KEY_GOAL = "goal";
	public static final String KEY_HEADLINE_LEAD = "headline_lead";
	public static final String KEY_HEADLINE_STAT = "headline_stat";
	public static final String KEY_RESULTS_LABEL = "results_label";
	public static final String KEY_RESULTS_TEXT = "results_text";
	public static final String KEY_DRIVERS_LABEL = "drivers_label";

	/** Key patterns of the numbered fields. */
	public static final String TACTIC_TITLE_KEY = "tactic_%d_title";
	public static final String TACTIC_TEXT_KEY = "tactic_%d_text";
	public static final String PLAN_TITLE_KEY = "plan_%d_title";
	public static final String PLAN_TEXT_KEY = "plan_%d_text";
	public static final String DRIVER_KEY = "driver_%d";
	public static final String STAT_VALUE_KEY = "stat_%d_value";
	public static final String STAT_LABEL_KEY = "stat_%d_label";

	/** Slot patterns of the numbered icon slots. */
	public static final String TACTIC_ICON_SLOT = "tactic_%d_icon";
	public static final String DRIVER_ICON_SLOT = "driver_%d_icon";
	public static final String STAT_ICON_SLOT = "stat_%d_icon";
	public static final String HEADLINE_ICON_SLOT = "headline_icon_%d";
	public static final String PLAN_ICON_SLOT = "plan_icon";

	/** How many of each numbered element the template holds. */
	public static final int TACTIC_COUNT = 4;
	public static final int PLAN_STEP_COUNT = 4;
	public static final int DRIVER_COUNT = 5;
	public static final int STAT_COUNT = 3;
	public static final int HEADLINE_ICON_COUNT = 2;
	public static final int STAT_ICON_COUNT = 2;

	/**
	 * Fields that state plain facts about the campaign. They are never lengthened with
	 * interpretation; when the material has no more detail, they stay short and are flagged.
	 */
	public static final Set<String> FACTS_ONLY_KEYS = Set.of(
			KEY_CLIENT, KEY_CHANNEL, KEY_FLIGHT, KEY_GOAL, "stat_1_label", "stat_2_label", "stat_3_label");

	/** Character safety ceiling for measured fields (well above any box's capacity). */
	public static final int BOX_SAFETY_CHARS = 600;

	/**
	 * Measured boxes per field, from the app's copy of the template (fonts, sizes, insets, and the
	 * space before the next element below each auto-growing box). Line targets follow the
	 * designer's original case study, which was written to fill each box; the five drivers share
	 * one text box aligned to five icons, so each must be exactly two lines to stay in line.
	 */
	public static final Map<String, OnePageTextBox> BOXES = Map.ofEntries(
			Map.entry(KEY_TITLE_LINE_1, new OnePageTextBox(FONT_BARLOW_BLACK, 41.12, 497.9, 0, 1, null)),
			Map.entry(KEY_TITLE_LINE_2, new OnePageTextBox(FONT_BARLOW_BLACK, 41.12, 497.9, 0, 1, null)),
			Map.entry(KEY_DESCRIPTION, new OnePageTextBox(FONT_INTER_LIGHT, 10, 252.7, 2, 2, null)),
			Map.entry(KEY_MAIN_TOPIC, new OnePageTextBox(FONT_BARLOW_EXTRA_BOLD, 7.35, 83.7, 0, 1, null)),
			Map.entry(KEY_CHALLENGE, new OnePageTextBox(FONT_INTER_LIGHT, 12, 302.5, 5, 6, null)),
			Map.entry(KEY_CLIENT, new OnePageTextBox(FONT_INTER_REGULAR, 9, 108.1, 3, 4, null)),
			Map.entry(KEY_CHANNEL, new OnePageTextBox(FONT_INTER_REGULAR, 9, 92.1, 0, 3, null)),
			Map.entry(KEY_FLIGHT, new OnePageTextBox(FONT_INTER_REGULAR, 9, 103.0, 0, 2, null)),
			Map.entry(KEY_GOAL, new OnePageTextBox(FONT_INTER_REGULAR, 9, 123.7, 2, 3, null)),
			Map.entry("tactic_1_title", new OnePageTextBox(FONT_BARLOW_BOLD, 12, 167.3, 0, 1, null)),
			Map.entry("tactic_1_text", new OnePageTextBox(FONT_INTER_LIGHT, 8, 133.7, 2, 2, null)),
			Map.entry("tactic_2_title", new OnePageTextBox(FONT_BARLOW_BOLD, 12, 167.3, 0, 1, null)),
			Map.entry("tactic_2_text", new OnePageTextBox(FONT_INTER_REGULAR, 8, 124.5, 2, 3, null)),
			Map.entry("tactic_3_title", new OnePageTextBox(FONT_BARLOW_BOLD, 12, 167.3, 0, 1, null)),
			Map.entry("tactic_3_text", new OnePageTextBox(FONT_INTER_LIGHT, 8, 115.4, 2, 2, null)),
			Map.entry("tactic_4_title", new OnePageTextBox(FONT_BARLOW_BOLD, 12, 167.3, 0, 1, null)),
			Map.entry("tactic_4_text", new OnePageTextBox(FONT_INTER_LIGHT, 8, 119.6, 2, 3, null)),
			Map.entry(KEY_HEADLINE_LEAD, new OnePageTextBox(FONT_BARLOW_EXTRA_BOLD, 15.17, 286.3, 0, 1, null)),
			Map.entry(KEY_HEADLINE_STAT, new OnePageTextBox(FONT_INTER_BLACK, 16.5, 156.1, 4, 5, null)),
			Map.entry(KEY_RESULTS_LABEL, new OnePageTextBox(FONT_BARLOW_EXTRA_BOLD, 24.17, 234.8, 0, 1, null)),
			Map.entry(KEY_RESULTS_TEXT, new OnePageTextBox(FONT_INTER_BOLD, 10, 283.6, 2, 2, null)),
			Map.entry("plan_1_title", new OnePageTextBox(FONT_BARLOW_EXTRA_BOLD, 15.17, 213.0, 0, 1, null)),
			Map.entry("plan_1_text", new OnePageTextBox(FONT_ROBOTO_REGULAR, 9.75, 200.1, 2, 2, null)),
			Map.entry("plan_2_title", new OnePageTextBox(FONT_BARLOW_EXTRA_BOLD, 15.17, 213.5, 0, 1, null)),
			Map.entry("plan_2_text", new OnePageTextBox(FONT_ROBOTO_REGULAR, 9.75, 232.6, 2, 2, null)),
			Map.entry("plan_3_title", new OnePageTextBox(FONT_BARLOW_EXTRA_BOLD, 15.17, 213.5, 0, 1, null)),
			Map.entry("plan_3_text", new OnePageTextBox(FONT_ROBOTO_REGULAR, 9.75, 202.9, 2, 2, null)),
			Map.entry("plan_4_title", new OnePageTextBox(FONT_BARLOW_EXTRA_BOLD, 15.17, 213.0, 0, 1, null)),
			Map.entry("plan_4_text", new OnePageTextBox(FONT_ROBOTO_REGULAR, 9.75, 187.6, 2, 2, null)),
			Map.entry(KEY_DRIVERS_LABEL, new OnePageTextBox(FONT_BARLOW_BLACK, 15.17, 161.7, 0, 1, null)),
			Map.entry("driver_1", new OnePageTextBox(FONT_INTER_REGULAR, 11, 142.8, 2, 2, null)),
			Map.entry("driver_2", new OnePageTextBox(FONT_INTER_REGULAR, 11, 142.8, 2, 2, null)),
			Map.entry("driver_3", new OnePageTextBox(FONT_INTER_REGULAR, 11, 142.8, 2, 2, null)),
			Map.entry("driver_4", new OnePageTextBox(FONT_INTER_REGULAR, 11, 142.8, 2, 2, null)),
			Map.entry("driver_5", new OnePageTextBox(FONT_INTER_REGULAR, 11, 142.8, 2, 2, null)),
			Map.entry("stat_1_label", new OnePageTextBox(FONT_BARLOW_BOLD, 13.17, 107.7, 0, 2, "stat_1_value")),
			Map.entry("stat_2_label", new OnePageTextBox(FONT_BARLOW_BOLD, 13.17, 114.3, 0, 2, "stat_2_value")),
			Map.entry("stat_3_label", new OnePageTextBox(FONT_BARLOW_BOLD, 13.17, 115.4, 0, 2, "stat_3_value")));

	/** Example tactics from the template's own case study (format only). */
	public static final String[][] TACTIC_EXAMPLES = {
			{"TACTICAL REALLOCATION", "Reallocated budget from high-CPA In-Feed ads into Demand Gen."},
			{"CREATIVE SEQUENCING", "Swapped generic messaging for offer-driven seasonal holiday campaigns."},
			{"DURATION ALIGNMENT", "Matched ad length to funnel goals: 30s for conversions, 6s for reach."},
			{"GEOGRAPHIC PRIORITIZATION", "Focused spend strictly on top-performing, high-efficiency Midwest markets."},
	};

	/** Example game-plan steps from the template's own case study (format only). */
	public static final String[][] PLAN_EXAMPLES = {
			{"CHANNEL ROLES", "Reposition In-Stream for awareness while scaling Demand Gen for direct conversions."},
			{"GEOGRAPHIC FOCUS", "Target spend exclusively on top-performing regional markets."},
			{"SEASONAL CREATIVE", "Align offer-driven promotions with key seasonal sales moments."},
			{"PERFORMANCE LOOPS", "Run monthly reviews to recalibrate format, creative, and geographic weighting."},
	};

	/** Example drivers from the template's own case study (format only). */
	public static final String[] DRIVER_EXAMPLES = {
			"80.23% VCR captured upper-funnel awareness", "Upper-funnel views fed conversions",
			"Urgent promos beat generic ads", "Summer Closeout hit $160 CPA", "Geo-targeting halved local CPAs",
	};

	/** Example stats from the template's own case study (format only). */
	public static final String[][] STAT_EXAMPLES = {{"-68%", "DEM-GEN CPA"}, {"+32%", "CONVERSIONS"},
			{"-67%", "OVERALL CPA"}};

	private OnePageCaseStudyTemplate() {
	}
}
