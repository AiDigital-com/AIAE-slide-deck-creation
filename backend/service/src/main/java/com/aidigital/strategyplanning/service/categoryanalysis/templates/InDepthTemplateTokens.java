package com.aidigital.strategyplanning.service.categoryanalysis.templates;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthSlideSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical token registry for the In-Depth Category Analysis Google Slides template.
 *
 * <p>Unlike the 5-slide Standard template, this master deck is a slide-layout library: 24
 * slides covering roughly 10 distinct content topics, several of which appear twice or more
 * as alternate visual layouts of the <em>same</em> content (e.g. slides 3/4, or slides 10-13
 * which split into a "full" and "lite" version of one topic). The review UI presents all 24
 * as a flat "check to include" list — every slide is independently checkable, duplicates
 * included — but a token is drafted <strong>once</strong> per distinct piece of content and
 * then applied to every checked slide that shares it, via {@link InDepthTokenSpec#slideNumbers()}.
 *
 * <p>As with {@link StandardTemplateTokens}, the deck filler replaces both the {@code {{token}}}
 * form and the sample literal currently in the master template, and each replacement is scoped
 * to the specific slide(s) it belongs to so identical sample text elsewhere in the deck is left
 * alone. Lookups and formatting live in
 * {@link com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthTemplateTokenRegistry}
 * and {@link com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthValueFormatter};
 * this class only holds the template facts.
 */
public final class InDepthTemplateTokens {

	/**
	 * All 24 slides of the master template, in deck order. Two are fixed section dividers with
	 * no drafted content ({@code APPENDIX}, {@code VISUAL ELEMENTS LIBRARY}) and are unchecked by
	 * default; the rest default to checked.
	 */
	public static final List<InDepthSlideSpec> SLIDES = List.of(
			new InDepthSlideSpec(1, "Cover — Category Analysis", true),
			new InDepthSlideSpec(2, "Market Trend — enrollment bar chart + 2 stats", true),
			new InDepthSlideSpec(3, "Category Highlights — layout A (stat + 4 bullets)", true),
			new InDepthSlideSpec(4, "Category Highlights — layout B (same content, alt layout)", false),
			new InDepthSlideSpec(5, "Market Highlights — layout A (4 stat callouts)", true),
			new InDepthSlideSpec(6, "Market Highlights — layout B (same content, alt order)", false),
			new InDepthSlideSpec(7, "Market Highlights — layout C (same content, alt order)", false),
			new InDepthSlideSpec(8, "Key Drivers — layout A (trip-type tags + 4 drivers)", true),
			new InDepthSlideSpec(9, "Key Drivers — layout B (same content, alt layout)", false),
			new InDepthSlideSpec(10, "Purchase Drivers — full, layout A (5 driver stats + 2 base stats)", true),
			new InDepthSlideSpec(11, "Purchase Drivers — full, layout B (same content, alt order)", false),
			new InDepthSlideSpec(12, "Purchase Drivers — lite, layout A (2 base stats only)", false),
			new InDepthSlideSpec(13, "Purchase Drivers — lite, layout B (same content, alt layout)", false),
			new InDepthSlideSpec(14, "Channel/Media Opportunity — layout A (2 channels: stat + insight each)", true),
			new InDepthSlideSpec(15, "Channel/Media Opportunity — layout B (same content, alt layout)", false),
			new InDepthSlideSpec(16, "Highlights & Implications (4 highlight → implication pairs)", true),
			new InDepthSlideSpec(17, "Section divider — Appendix", false),
			new InDepthSlideSpec(18, "Category Stat Chart — full (4 stats + 7-bar breakdown)", true),
			new InDepthSlideSpec(19, "Category Stat Chart — lite, layout A (4 stats only)", false),
			new InDepthSlideSpec(20, "Category Stat Chart — lite, layout B (same content, alt order)", false),
			new InDepthSlideSpec(21, "Awareness-to-Action Journey (Understand/Evaluate/Act + 3 stats)", true),
			new InDepthSlideSpec(22, "Seasonal Decision Journey (5-stage funnel)", false),
			new InDepthSlideSpec(23, "Regulatory Landscape Map (by U.S. state)", false),
			new InDepthSlideSpec(24, "Section divider — Visual Elements Library", false));

	/**
	 * Fields the master styles in ALL CAPS; drafted values are upper-cased to match.
	 */
	public static final Set<String> ALL_CAPS_KEYS = Set.of(
			"hi_highlight_1_title", "hi_highlight_2_title", "hi_highlight_3_title", "hi_highlight_4_title",
			"hi_implication_1_title", "hi_implication_2_title", "hi_implication_3_title",
			"hi_implication_4_title",
			"channel_1_name", "channel_2_name",
			"feed_stage_1_label", "feed_stage_2_label", "feed_stage_3_label", "feed_stage_4_label",
			"feed_stage_5_label");

	/**
	 * Donut slices — label/value pairs sorted largest first before the deck is filled.
	 */
	public static final int DONUT_SLICES = 4;

	/**
	 * Slide-18 bars — label/value pairs sorted smallest first (bar 1 = bottom bar).
	 */
	public static final int STAT_CHART_BARS = 7;

	/**
	 * Values that only size a chart; they are never written into a text box.
	 */
	public static final Set<String> CHART_DATA_KEYS = Set.of(
			"donut_1_value", "donut_2_value", "donut_3_value", "donut_4_value");

	/**
	 * Percentage fields whose master box prints the "%" itself right after the token
	 * ({@code {{stat_1_value}}%} on slides 18-20), so the value goes in as a bare number.
	 */
	public static final Set<String> PERCENT_SIGN_IN_MASTER_KEYS = Set.of(
			"stat_1_value", "stat_2_value", "stat_3_value", "stat_4_value",
			"stat_chart_bar_1_value", "stat_chart_bar_2_value", "stat_chart_bar_3_value",
			"stat_chart_bar_4_value", "stat_chart_bar_5_value", "stat_chart_bar_6_value",
			"stat_chart_bar_7_value");

	/**
	 * Fields that end in a period on every layout (slides 5-7 stat labels), so all boxes match.
	 */
	public static final Set<String> SENTENCE_KEYS = Set.of(
			"market_stat_1_label", "market_stat_2_label", "market_stat_3_label", "market_stat_4_label");

	/**
	 * Slide-21 stage-title keys whose text box was manually resized to the width of the journey
	 * column, so they get a fixed character budget instead of the sample-length formula.
	 */
	public static final Set<String> JOURNEY_STAGE_TITLE_KEYS = Set.of(
			"journey_understand_title", "journey_evaluate_title", "journey_act_title");

	/**
	 * Keys whose master boxes hold {@code {{token}}} placeholders and must never fall back to
	 * matching sample text: the slide-21 stage titles ("ACT" is too common a word), plus every
	 * field on the fully tokenized slides 8 (donut labels), 14-16, and 18-20, and the slide-22
	 * funnel labels. On those slides a sample like "30%" or "Other" could match text that an
	 * earlier replacement just wrote, and the master's duplicate "of U.S. Hispanic adults…" line
	 * would write one value into two boxes.
	 */
	public static final Set<String> PLACEHOLDER_ONLY_KEYS;

	static {
		Set<String> keys = new LinkedHashSet<>(List.of(
				"journey_understand_title", "journey_evaluate_title", "journey_act_title",
				"stat_headline", "stat_chart_subheadline", "stat_chart_note"));
		for (int n = 1; n <= 2; n++) {
			keys.addAll(List.of("channel_" + n + "_name", "channel_" + n + "_stat_value",
					"channel_" + n + "_stat_label", "channel_" + n + "_insight"));
		}
		for (int n = 1; n <= 4; n++) {
			keys.addAll(List.of("donut_" + n + "_label", "stat_" + n + "_value", "stat_" + n + "_label",
					"hi_highlight_" + n + "_title", "hi_highlight_" + n + "_text",
					"hi_implication_" + n + "_title", "hi_implication_" + n + "_text"));
		}
		for (int n = 1; n <= 5; n++) {
			keys.add("feed_stage_" + n + "_label");
		}
		for (int n = 1; n <= STAT_CHART_BARS; n++) {
			keys.addAll(List.of("stat_chart_bar_" + n + "_label", "stat_chart_bar_" + n + "_value"));
		}
		PLACEHOLDER_ONLY_KEYS = Set.copyOf(keys);
	}

	/**
	 * All tokens of the In-Depth Category Analysis template, grouped by topic.
	 */
	public static final List<InDepthTokenSpec> TOKENS = List.of(

			// ---- Slide 2 — Market trend (bar chart + 2 stats) ----
			new InDepthTokenSpec("trend_headline", "Headline", List.of(2),
					"NAVIGATING A GROWING MEDICARE ADVANTAGE"),
			new InDepthTokenSpec("trend_intro", "Intro paragraph", List.of(2),
					"Medicare and Medicaid audiences are navigating a short enrollment window, growing plan "
							+ "choice, and complex benefit decisions, making Spanish-first outreach critical "
							+ "during AEP."),
			new InDepthTokenSpec("trend_bullet_1", "Trend bullet 1", List.of(2),
					"Medicare Advantage is now a majority-share Medicare option, making AEP a more "
							+ "competitive enrollment moment."),
			new InDepthTokenSpec("trend_bullet_2", "Trend bullet 2", List.of(2),
					"Dual-eligible consumers represent a meaningful audience with distinct needs around "
							+ "affordability, benefits, and eligibility clarity."),
			new InDepthTokenSpec("trend_bullet_3", "Trend bullet 3", List.of(2),
					"As plan choice expands, the opportunity is not just to reach consumers, but to make "
							+ "enrollment information feel simple, relevant, and actionable."),
			// Group label above the 3 trend bullets — same previously-untokenized gap as
			// slide 8's "TRIP TYPES" and slide 16's "HIGHLIGHTS"/"IMPLICATIONS".
			new InDepthTokenSpec("trend_bullets_label", "Trend bullets group label", List.of(2), "WHAT’S TRENDING?"),
			new InDepthTokenSpec("trend_chart_title", "Bar chart title", List.of(2),
					"MEDICARE ADVANTAGE ENROLLMENT RATE"),
			new InDepthTokenSpec("trend_point_1_period", "Chart point 1 — period", List.of(2), "2015"),
			new InDepthTokenSpec("trend_point_1_value", "Chart point 1 — value", List.of(2), "32%"),
			new InDepthTokenSpec("trend_point_2_period", "Chart point 2 — period", List.of(2), "2017"),
			new InDepthTokenSpec("trend_point_2_value", "Chart point 2 — value", List.of(2), "34%"),
			new InDepthTokenSpec("trend_point_3_period", "Chart point 3 — period", List.of(2), "2019"),
			new InDepthTokenSpec("trend_point_3_value", "Chart point 3 — value", List.of(2), "37%"),
			new InDepthTokenSpec("trend_point_4_period", "Chart point 4 — period", List.of(2), "2021"),
			new InDepthTokenSpec("trend_point_4_value", "Chart point 4 — value", List.of(2), "43%"),
			new InDepthTokenSpec("trend_point_5_period", "Chart point 5 — period", List.of(2), "2023"),
			new InDepthTokenSpec("trend_point_5_value", "Chart point 5 — value", List.of(2), "51%"),
			new InDepthTokenSpec("trend_point_6_period", "Chart point 6 — period", List.of(2), "2024"),
			new InDepthTokenSpec("trend_point_6_value", "Chart point 6 — value", List.of(2), "52%"),
			new InDepthTokenSpec("trend_point_7_period", "Chart point 7 — period", List.of(2), "2025"),
			new InDepthTokenSpec("trend_point_7_value", "Chart point 7 — value", List.of(2), "54%"),
			new InDepthTokenSpec("trend_stat_1_value", "Stat 1 — value", List.of(2), "11.9M"),
			new InDepthTokenSpec("trend_stat_1_label", "Stat 1 — label", List.of(2),
					"people had both Medicare and Medicaid coverage in 2025"),
			new InDepthTokenSpec("trend_stat_2_value", "Stat 2 — value", List.of(2), "54%"),
			new InDepthTokenSpec("trend_stat_2_label", "Stat 2 — label", List.of(2),
					"of eligible Medicare beneficiaries are enrolled in Medicare Advantage plans in 2025"),
			new InDepthTokenSpec("trend_sources", "Sources", List.of(2),
					"Source: KFF, Medicare Advantage in 2025; CMS, Medicare Open Enrollment Partner Resources."),

			// ---- Slides 3-4 — Category highlights (stat + 4 bullets), 2 layouts of the same content ----
			new InDepthTokenSpec("highlights_headline", "Headline", List.of(3, 4),
					"U.S. PEDIATRIC HEALTHCARE HIGHLIGHTS"),
			new InDepthTokenSpec("highlights_intro", "Intro paragraph", List.of(3, 4),
					"Rising demand for specialized pediatric care, preventative services, and regional "
							+ "centers of excellence is reshaping how families choose and engage."),
			new InDepthTokenSpec("highlights_stat_value", "Stat — value", List.of(3, 4), "$52.6B"),
			new InDepthTokenSpec("highlights_stat_label", "Stat — label", List.of(3, 4),
					"U.S. pediatric hospitals market revenue in 2023"),
			new InDepthTokenSpec("highlights_bullet_1", "Bullet 1", List.of(3, 4),
					"Pediatric healthcare is shifting from episodic treatment to ongoing, relationship-based "
							+ "care, emphasizing trust, reputation, and continuity."),
			new InDepthTokenSpec("highlights_bullet_2", "Bullet 2", List.of(3, 4),
					"Families increasingly seek specialized children’s hospitals for complex and "
							+ "preventative care, rather than general healthcare settings."),
			new InDepthTokenSpec("highlights_bullet_3", "Bullet 3", List.of(3, 4),
					"Growth in outpatient, diagnostic, and preventative services is expanding the role of "
							+ "children’s hospitals beyond emergency care."),
			new InDepthTokenSpec("highlights_bullet_4", "Bullet 4", List.of(3, 4),
					"Digital engagement and research behaviors now play a larger role in how parents "
							+ "evaluate and select pediatric providers."),
			new InDepthTokenSpec("highlights_section_label", "Section label", List.of(3, 4),
					"PEDIATRIC CARE MARKET TRENDS"),

			// ---- Slides 5-7 — Market highlights (4 stat callouts), 3 layouts of the same content ----
			new InDepthTokenSpec("market_headline", "Headline", List.of(5, 6, 7),
					"BOOT RETAIL IS EXPANDING"),
			new InDepthTokenSpec("market_intro", "Intro paragraph", List.of(5, 6, 7),
					"U.S. boot brands are expanding their physical store footprint, driven by steady "
							+ "consumer demand for in-person fitting, craftsmanship, and brand experience."),
			new InDepthTokenSpec("market_stat_1_value", "Stat 1 — value", List.of(5, 6, 7), "70"),
			new InDepthTokenSpec("market_stat_1_label", "Stat 1 — label", List.of(5, 6, 7),
					"New stores planned by Boot Barn in FY2026, reflecting continued nationwide expansion."),
			new InDepthTokenSpec("market_stat_2_value", "Stat 2 — value", List.of(5, 6, 7), "5M"),
			new InDepthTokenSpec("market_stat_2_label", "Stat 2 — label", List.of(5, 6, 7),
					"Investment in a single new Cavender’s store, reflecting strong retailer confidence "
							+ "in physical boot sales."),
			new InDepthTokenSpec("market_stat_3_value", "Stat 3 — value", List.of(5, 6, 7), "1,200"),
			new InDepthTokenSpec("market_stat_3_label", "Stat 3 — label", List.of(5, 6, 7),
					"Revised long-term U.S. store potential for Boot Barn, showing confidence in category "
							+ "growth."),
			new InDepthTokenSpec("market_stat_4_value", "Stat 4 — value", List.of(5, 6, 7), "42+"),
			new InDepthTokenSpec("market_stat_4_label", "Stat 4 — label", List.of(5, 6, 7),
					"Tecovas retail locations following its transition from digital-first to physical retail."),
			new InDepthTokenSpec("market_sources", "Sources", List.of(5, 6, 7),
					"Source: Boot Barn Holdings Inc. Investor Relations; Modern Retail; MySanAntonio; "
							+ "RGV Business Journal"),

			// ---- Slides 8-9 — Key drivers (trip-type tags + 4 drivers), 2 layouts ----
			new InDepthTokenSpec("drivers_headline", "Headline", List.of(8, 9),
					"NEARBY TRAVEL TRENDS"),
			new InDepthTokenSpec("drivers_intro", "Intro paragraph", List.of(8, 9),
					"Across life stages, travelers are prioritizing trips that feel simple to plan, "
							+ "flexible, and worth the spend, especially experiences that combine outdoor "
							+ "activity, quality time, and lower-crowd environments."),
			new InDepthTokenSpec("drivers_section_title", "Drivers section title", List.of(8, 9),
					"KEY DRIVERS OF REGIONAL TRAVEL"),
			// Title of the slide-8 donut card ("TRIP TYPES" in the master).
			new InDepthTokenSpec("drivers_tags_label", "Donut chart title", List.of(8), "TRIP TYPES"),
			// Slide 8 donut: 4 slices. The deck builder sorts the pairs by value so slice 1 is
			// always the largest, drawn first from 12 o'clock and continuing clockwise; the legend
			// label boxes {{donut_N_label}} follow the same order. Values are chart data only (no
			// text box shows them) — see CHART_DATA_KEYS.
			new InDepthTokenSpec("donut_1_label", "Donut slice 1 — label", List.of(8), "Day Trips"),
			new InDepthTokenSpec("donut_1_value", "Donut slice 1 — share of total (%)", List.of(8), "38%"),
			new InDepthTokenSpec("donut_2_label", "Donut slice 2 — label", List.of(8), "Microcations (1-3 days)"),
			new InDepthTokenSpec("donut_2_value", "Donut slice 2 — share of total (%)", List.of(8), "27%"),
			new InDepthTokenSpec("donut_3_label", "Donut slice 3 — label", List.of(8), "Long Trips (+4 days)"),
			new InDepthTokenSpec("donut_3_value", "Donut slice 3 — share of total (%)", List.of(8), "21%"),
			new InDepthTokenSpec("donut_4_label", "Donut slice 4 — label", List.of(8), "Seasonal Trips"),
			new InDepthTokenSpec("donut_4_value", "Donut slice 4 — share of total (%)", List.of(8), "14%"),
			new InDepthTokenSpec("driver_1_title", "Driver 1 — title", List.of(8, 9), "Outdoor & Nature Appeal"),
			new InDepthTokenSpec("driver_1_text", "Driver 1 — text", List.of(8, 9),
					"Fresh air, scenic views, and simple escapes drive strong interest in open-air destinations."),
			new InDepthTokenSpec("driver_2_title", "Driver 2 — title", List.of(8, 9), "Shared Experiences"),
			new InDepthTokenSpec("driver_2_text", "Driver 2 — text", List.of(8, 9),
					"Travelers are looking for memory-making moments, from family time to couples trips "
							+ "and pet-friendly escapes."),
			new InDepthTokenSpec("driver_3_title", "Driver 3 — title", List.of(8, 9), "Easy, Flexible Planning"),
			new InDepthTokenSpec("driver_3_text", "Driver 3 — text", List.of(8, 9),
					"Shorter travel windows, driveability, and simple itineraries make regional trips "
							+ "easier to say yes to."),
			new InDepthTokenSpec("driver_4_title", "Driver 4 — title", List.of(8, 9), "Value Without Sacrifice"),
			new InDepthTokenSpec("driver_4_text", "Driver 4 — text", List.of(8, 9),
					"Regional getaways offer the feeling of a real escape while helping travelers manage "
							+ "cost, time, and convenience."),
			new InDepthTokenSpec("drivers_sources", "Sources", List.of(8, 9),
					"Source: NYU SPS / Family Travel Association 2025 Family Travel Survey; Expedia Group "
							+ "Unpack ’25; U.S. Travel Association"),

			// ---- Slides 10-13 — Purchase drivers: shared fields (full + lite layouts) ----
			new InDepthTokenSpec("purchase_headline", "Headline", List.of(10, 11, 12, 13),
					"RESEARCH DRIVING TABLET PURCHASES"),
			new InDepthTokenSpec("purchase_intro", "Intro paragraph", List.of(10, 11, 12, 13),
					"Buyers researching note-taking and digital paper tablets rely heavily on reviews, "
							+ "comparison videos, and product demos to evaluate writing experience and display "
							+ "quality."),
			new InDepthTokenSpec("purchase_base_stat_1_value", "Base stat 1 — value", List.of(10, 11, 12, 13), "81%"),
			new InDepthTokenSpec("purchase_base_stat_1_label", "Base stat 1 — label", List.of(10, 11, 12, 13),
					"Consumers research consumer electronics online before making a purchase."),
			new InDepthTokenSpec("purchase_base_stat_2_value", "Base stat 2 — value", List.of(10, 11, 12, 13), "70%"),
			new InDepthTokenSpec("purchase_base_stat_2_label", "Base stat 2 — label", List.of(10, 11, 12, 13),
					"Technology buyers watch online video reviews before deciding which device to buy."),
			new InDepthTokenSpec("purchase_sources", "Sources", List.of(10, 11, 12, 13),
					"Source: Google / Think with Google Consumer Tech Research, Statista Global Consumer Survey"),
			// ---- Slides 10-11 only — 5 additional purchase-driver stats ----
			new InDepthTokenSpec("purchase_drivers_section_title", "Drivers section title", List.of(10, 11),
					"PRIMARY PURCHASE DRIVERS FOR  NOTE-TAKING TABLETS"),
			new InDepthTokenSpec("purchase_driver_1_label", "Driver 1 — label", List.of(10, 11),
					"Organization and note management features"),
			new InDepthTokenSpec("purchase_driver_1_value", "Driver 1 — value", List.of(10, 11), "48%"),
			new InDepthTokenSpec("purchase_driver_2_label", "Driver 2 — label", List.of(10, 11),
					"Stylus precision and handwriting accuracy"),
			new InDepthTokenSpec("purchase_driver_2_value", "Driver 2 — value", List.of(10, 11), "55%"),
			new InDepthTokenSpec("purchase_driver_3_label", "Driver 3 — label", List.of(10, 11), "Long battery life"),
			new InDepthTokenSpec("purchase_driver_3_value", "Driver 3 — value", List.of(10, 11), "62%"),
			new InDepthTokenSpec("purchase_driver_4_label", "Driver 4 — label", List.of(10, 11),
					"Eye-friendly display for reading and writing"),
			new InDepthTokenSpec("purchase_driver_4_value", "Driver 4 — value", List.of(10, 11), "66%"),
			new InDepthTokenSpec("purchase_driver_5_label", "Driver 5 — label", List.of(10, 11),
					"Paper-like writing experience"),
			new InDepthTokenSpec("purchase_driver_5_value", "Driver 5 — value", List.of(10, 11), "71%"),

			// ---- Slides 14-15 — Channel/media opportunity, 2 layouts ----
			// Two channel columns (CTV and AUDIO in the master), each with a channel name, one stat
			// about the audience's use of that channel, and a so-what insight for that channel.
			// The name and stat-label keys are placeholder-only (see PLACEHOLDER_ONLY_KEYS in
			// GoogleDeckServiceImpl): "CTV" collides with drafted text, and the master holds the
			// same stat-label sample twice, so a literal fallback would write one value to both.
			new InDepthTokenSpec("channel_headline", "Headline", List.of(14, 15),
					"STREAMING TO SPANISH-LANGUAGE AUDIENCES"),
			new InDepthTokenSpec("channel_intro", "Intro paragraph", List.of(14, 15),
					"Hispanic audiences are shaping their own media experiences across streaming video, "
							+ "digital audio, and digital content."),
			new InDepthTokenSpec("channel_1_name", "Channel 1 — name (programmatic channel, e.g. CTV)",
					List.of(14, 15), "CTV"),
			new InDepthTokenSpec("channel_1_stat_value", "Channel 1 — stat value", List.of(14, 15), "87%"),
			new InDepthTokenSpec("channel_1_stat_label", "Channel 1 — stat label (audience use of channel 1)",
					List.of(14, 15), "of U.S. Hispanic adults get news from digital devices"),
			// Sample matches slide 14's literal text, including its "Spanish- language" break.
			new InDepthTokenSpec("channel_1_insight", "Channel 1 — insight (what channel 1 does for the client)",
					List.of(14, 15),
					"can build repeated exposure in premium Spanish- language environments, helping "
							+ "United Healthcare explain plan options before audiences are ready to take action."),
			new InDepthTokenSpec("channel_2_name", "Channel 2 — name (programmatic channel, e.g. AUDIO)",
					List.of(14, 15), "AUDIO"),
			new InDepthTokenSpec("channel_2_stat_value", "Channel 2 — stat value", List.of(14, 15), "56%"),
			new InDepthTokenSpec("channel_2_stat_label", "Channel 2 — stat label (audience use of channel 2)",
					List.of(14, 15), "of U.S. Hispanic adults get news from digital devices"),
			new InDepthTokenSpec("channel_2_insight", "Channel 2 — insight (what channel 2 does for the client)",
					List.of(14, 15),
					"Streaming audio can reinforce Spanish-first messaging throughout the day, while "
							+ "display retargeting can bring interested users back"),
			new InDepthTokenSpec("channel_sources", "Sources", List.of(14, 15),
					"Source: Google / Think with Google Consumer Tech Research, Statista Global Consumer Survey"),

			// ---- Slide 16 — Highlights & implications (4 rows: highlight N sets up implication N) ----
			new InDepthTokenSpec("hi_headline", "Headline", List.of(16),
					"WHAT THIS MEANS FOR ALBERTA BOOT’S US EXPANSION"),
			// The "HIGHLIGHTS" / "IMPLICATIONS" column labels are fixed template text, never drafted.
			// Four rows, each a highlight (left) paired with the implication it sets up (right).
			// Samples are the master's literal ALL-CAPS titles, in the master's row order, so the
			// literal fallback matches until the master holds {{token}} placeholders.
			new InDepthTokenSpec("hi_highlight_1_title", "Row 1 highlight — title", List.of(16),
					"FUNCTIONAL DEMAND ANCHORS THE CATEGORY"),
			new InDepthTokenSpec("hi_highlight_1_text", "Row 1 highlight — text", List.of(16),
					"Work-driven industries sustain consistent boot purchases year-round, creating a "
							+ "stable demand base beyond fashion cycles."),
			new InDepthTokenSpec("hi_implication_1_title", "Row 1 implication — title", List.of(16),
					"SOCIAL DISCOVERY FUELS AWARENESS"),
			new InDepthTokenSpec("hi_implication_1_text", "Row 1 implication — text", List.of(16),
					"Boot demand is regionally concentrated, making geo targeted media in high-index and "
							+ "expansion markets more efficient."),
			new InDepthTokenSpec("hi_highlight_2_title", "Row 2 highlight — title", List.of(16),
					"WESTERN IDENTITY HAS BROADENED"),
			new InDepthTokenSpec("hi_highlight_2_text", "Row 2 highlight — text", List.of(16),
					"Western aesthetics now extend beyond ranch-driven consumers into lifestyle and "
							+ "fashion environments."),
			new InDepthTokenSpec("hi_implication_2_title", "Row 2 implication — title", List.of(16),
					"LEAD WITH MODERN HERITAGE"),
			new InDepthTokenSpec("hi_implication_2_text", "Row 2 implication — text", List.of(16),
					"Position Alberta Boot as authentic yet versatile, appealing to both core western and "
							+ "lifestyle audiences."),
			new InDepthTokenSpec("hi_highlight_3_title", "Row 3 highlight — title", List.of(16),
					"PREMIUM CRAFT DRIVES TRADE-UP"),
			new InDepthTokenSpec("hi_highlight_3_text", "Row 3 highlight — text", List.of(16),
					"Consumers increasingly value durability, full-grain leather, and long-term wear over "
							+ "fast fashion alternatives."),
			new InDepthTokenSpec("hi_implication_3_title", "Row 3 implication — title", List.of(16),
					"ELEVATE CRAFT THROUGH VIDEO"),
			new InDepthTokenSpec("hi_implication_3_text", "Row 3 implication — text", List.of(16),
					"Leverage CTV, YouTube, and premium placements to showcase craftsmanship before "
							+ "consumers enter search."),
			new InDepthTokenSpec("hi_highlight_4_title", "Row 4 highlight — title", List.of(16),
					"RETAIL EXPANSION SIGNALS CONFIDENCE"),
			new InDepthTokenSpec("hi_highlight_4_text", "Row 4 highlight — text", List.of(16),
					"Specialty boot retailers continue opening stores nationwide, reinforcing sustained "
							+ "category growth."),
			new InDepthTokenSpec("hi_implication_4_title", "Row 4 implication — title", List.of(16),
					"ACTIVATE AROUND SEASONAL PEAKS"),
			new InDepthTokenSpec("hi_implication_4_text", "Row 4 implication — text", List.of(16),
					"Flight media strategically into Q3 and Q4 when intent accelerates and retail traffic "
							+ "spikes."),

			// ---- Slides 18-20 — Category stat chart: shared fields (full + lite layouts) ----
			new InDepthTokenSpec("stat_headline", "Headline", List.of(18, 19, 20),
					"BLENDING PLANT AND ANIMAL DIETS"),
			new InDepthTokenSpec("stat_1_value", "Stat 1 — value", List.of(18, 19, 20), "95%"),
			new InDepthTokenSpec("stat_1_label", "Stat 1 — label", List.of(18, 19, 20),
					"of households that bought plant-based meat also bought animal-based meat"),
			new InDepthTokenSpec("stat_2_value", "Stat 2 — value", List.of(18, 19, 20), "44%"),
			new InDepthTokenSpec("stat_2_label", "Stat 2 — label", List.of(18, 19, 20),
					"of US households purchased plant-based milk"),
			new InDepthTokenSpec("stat_3_value", "Stat 3 — value", List.of(18, 19, 20), "30%"),
			new InDepthTokenSpec("stat_3_label", "Stat 3 — label", List.of(18, 19, 20),
					"Of consumers claimed that plant-based alternatives are healthier than animal-based meat"),
			new InDepthTokenSpec("stat_4_value", "Stat 4 — value", List.of(18, 19, 20), "25%"),
			new InDepthTokenSpec("stat_4_label", "Stat 4 — label", List.of(18, 19, 20),
					"Of US households purchased plant-based milk also purchased plant-based meat"),
			// ---- Slide 18 only — bar-chart subheadline + 7-category breakdown ----
			new InDepthTokenSpec("stat_chart_subheadline", "Bar-chart subheadline", List.of(18),
					"ALL-AMERICAN CLASSIC: BELOVED BURGER TOPS PLANT-BASED MEAT CHART"),
			new InDepthTokenSpec("stat_chart_note", "Bar-chart note", List.of(18),
					"Share of respondents among people who consumed plant-based alternatives in the past "
							+ "3 months"),
			new InDepthTokenSpec("stat_chart_bar_1_label", "Bar 1 — label", List.of(18), "Other"),
			new InDepthTokenSpec("stat_chart_bar_1_value", "Bar 1 — value", List.of(18), "18%"),
			new InDepthTokenSpec("stat_chart_bar_2_label", "Bar 2 — label", List.of(18), "Sliced Meat"),
			new InDepthTokenSpec("stat_chart_bar_2_value", "Bar 2 — value", List.of(18), "27%"),
			new InDepthTokenSpec("stat_chart_bar_3_label", "Bar 3 — label", List.of(18), "Minced meat"),
			new InDepthTokenSpec("stat_chart_bar_3_value", "Bar 3 — value", List.of(18), "30%"),
			new InDepthTokenSpec("stat_chart_bar_4_label", "Bar 4 — label", List.of(18), "Sausages"),
			new InDepthTokenSpec("stat_chart_bar_4_value", "Bar 4 — value", List.of(18), "33%"),
			new InDepthTokenSpec("stat_chart_bar_5_label", "Bar 5 — label", List.of(18), "Bacon"),
			new InDepthTokenSpec("stat_chart_bar_5_value", "Bar 5 — value", List.of(18), "35%"),
			new InDepthTokenSpec("stat_chart_bar_6_label", "Bar 6 — label", List.of(18),
					"Chicken (e.g. nuggets, strips)"),
			new InDepthTokenSpec("stat_chart_bar_6_value", "Bar 6 — value", List.of(18), "45%"),
			new InDepthTokenSpec("stat_chart_bar_7_label", "Bar 7 — label", List.of(18), "Burgers"),
			new InDepthTokenSpec("stat_chart_bar_7_value", "Bar 7 — value", List.of(18), "55%"),

			// ---- Slide 21 — Awareness-to-action journey (Understand/Evaluate/Act + 3 stats) ----
			new InDepthTokenSpec("journey_headline", "Headline", List.of(21), "CONVERTING AWARENESS TO ACTION"),
			new InDepthTokenSpec("journey_intro", "Intro paragraph", List.of(21),
					"Medicare and Medicaid decisions require plan education, language clarity, and "
							+ "repeated exposure. Spanish-first messaging can help audiences understand options "
							+ "before they are ready to take action."),
			// Stage title labels — the master's boxes now hold the literal "{{journey_..._title}}"
			// placeholder (not the bare word) because a common word like "ACT" is too collision-prone
			// to match safely as a literal-text fallback (see PLACEHOLDER_ONLY_KEYS in
			// GoogleDeckServiceImpl); the {{token}} form is the only match path for these three.
			// Box width fixed at ~2.5x the length of "Understand" — cap enforced in maxCharsFor.
			new InDepthTokenSpec("journey_understand_title", "Stage 1 title (Understand)", List.of(21), "UNDERSTAND"),
			new InDepthTokenSpec("journey_understand_1", "Understand — bullet 1", List.of(21),
					"Learn that AEP is the moment to review coverage"),
			new InDepthTokenSpec("journey_understand_2", "Understand — bullet 2", List.of(21),
					"Compare plan basics like cost, benefits, providers, and prescriptions"),
			new InDepthTokenSpec("journey_understand_3", "Understand — bullet 3", List.of(21),
					"Need simple Spanish-language explanations before deeper research"),
			new InDepthTokenSpec("journey_evaluate_title", "Stage 2 title (Evaluate)", List.of(21), "EVALUATE"),
			new InDepthTokenSpec("journey_evaluate_1", "Evaluate — bullet 1", List.of(21),
					"Revisit plan details and eligibility requirements"),
			new InDepthTokenSpec("journey_evaluate_2", "Evaluate — bullet 2", List.of(21),
					"Look for affordability, coverage, and provider access cues"),
			new InDepthTokenSpec("journey_evaluate_3", "Evaluate — bullet 3", List.of(21),
					"Benefit from repeated messaging across trusted media environments"),
			new InDepthTokenSpec("journey_act_title", "Stage 3 title (Act)", List.of(21), "ACT"),
			new InDepthTokenSpec("journey_act_1", "Act — bullet 1", List.of(21),
					"Visit the plan landing page or call for support"),
			new InDepthTokenSpec("journey_act_2", "Act — bullet 2", List.of(21),
					"Confirm eligibility and next steps"),
			new InDepthTokenSpec("journey_act_3", "Act — bullet 3", List.of(21),
					"Convert when the path is clear, compliant, and easy to navigate"),
			new InDepthTokenSpec("journey_stat_1_value", "Stat 1 — value", List.of(21), "95%"),
			new InDepthTokenSpec("journey_stat_1_label", "Stat 1 — label", List.of(21),
					"of Medicare Advantage enrollees are in Special Needs Plans"),
			new InDepthTokenSpec("journey_stat_2_value", "Stat 2 — value", List.of(21), "80%"),
			new InDepthTokenSpec("journey_stat_2_label", "Stat 2 — label", List.of(21),
					"of SNP enrollment is in D-SNPs for people dually eligible for Medicare and Medicaid"),
			new InDepthTokenSpec("journey_stat_3_value", "Stat 3 — value", List.of(21), "34"),
			new InDepthTokenSpec("journey_stat_3_label", "Stat 3 — label", List.of(21),
					"of adults with limited English say language made it hard to fill out health care forms"),
			new InDepthTokenSpec("journey_sources", "Sources", List.of(21),
					"Source: Google / Think with Google Consumer Tech Research, Statista Global Consumer Survey"),

			// ---- Slide 22 — Seasonal decision journey (5-stage funnel) ----
			new InDepthTokenSpec("feed_headline", "Headline", List.of(22), "THE SEASONAL FEED DECISION JOURNEY"),
			// Purchase-funnel stage labels beside each stage. Listed before the stage titles/texts
			// so their literal fallbacks run before any drafted text lands on the slide.
			new InDepthTokenSpec("feed_stage_1_label", "Stage 1 funnel label (awareness)", List.of(22),
					"SEASONAL AWARENESS"),
			new InDepthTokenSpec("feed_stage_2_label", "Stage 2 funnel label (consideration)", List.of(22),
					"CONSIDERATION"),
			new InDepthTokenSpec("feed_stage_3_label", "Stage 3 funnel label (purchase)", List.of(22),
					"PURCHASE MOMENT"),
			new InDepthTokenSpec("feed_stage_4_label", "Stage 4 funnel label (trade-up)", List.of(22), "TRADE-UP"),
			new InDepthTokenSpec("feed_stage_5_label", "Stage 5 funnel label (loyalty/maintenance)", List.of(22),
					"MAINTENANCE"),
			new InDepthTokenSpec("feed_intro", "Intro paragraph", List.of(22),
					"Feed demand shifts throughout the year as weather, routines, and animal needs change, "
							+ "creating predictable moments of peak relevance by category."),
			new InDepthTokenSpec("feed_stage_1_title", "Stage 1 title (Awareness)", List.of(22), "ANTICIPATE NEEDS"),
			new InDepthTokenSpec("feed_stage_1_text", "Stage 1 text (Awareness)", List.of(22),
					"Seasonal changes prompt owners to anticipate upcoming feeding needs tied to weather "
							+ "and animal life cycle."),
			new InDepthTokenSpec("feed_stage_2_title", "Stage 2 title (Consideration)", List.of(22),
					"EVALUATE OPTIONS"),
			new InDepthTokenSpec("feed_stage_2_text", "Stage 2 text (Consideration)", List.of(22),
					"Owners compare nutrition, price, and availability, relying on education and retailer "
							+ "guidance during seasonal transitions."),
			new InDepthTokenSpec("feed_stage_3_title", "Stage 3 title (Purchase)", List.of(22),
					"REPLENISH & STOCK UP"),
			new InDepthTokenSpec("feed_stage_3_text", "Stage 3 text (Purchase)", List.of(22),
					"Purchases concentrate in-store, with larger baskets during periods of peak seasonal "
							+ "dependence on feed."),
			new InDepthTokenSpec("feed_stage_4_title", "Stage 4 title (Trade-up)", List.of(22), "PREMIUM MOMENTS"),
			new InDepthTokenSpec("feed_stage_4_text", "Stage 4 text (Trade-up)", List.of(22),
					"Q4 and special occasions create opportunities for higher-margin pet food, treats, and "
							+ "specialty nutrition."),
			new InDepthTokenSpec("feed_stage_5_title", "Stage 5 title (Maintenance)", List.of(22), "ROUTINE FEEDING"),
			new InDepthTokenSpec("feed_stage_5_text", "Stage 5 text (Maintenance)", List.of(22),
					"Once routines are set, consistency and availability drive repeat purchasing with "
							+ "limited brand switching."),
			new InDepthTokenSpec("feed_sources", "Sources", List.of(22),
					"Source: APPA National Pet Owners Survey, Mintel Pet Food Trends, Statista"),

			// ---- Slide 23 — Regulatory landscape map: text fields only (see REGULATORY_MAP_SLIDE) ----
			new InDepthTokenSpec("map_headline", "Headline", List.of(23),
					"ONLINE GAMBLING REGULATIONS IN U.S."),
			new InDepthTokenSpec("map_subheadline", "Subheadline", List.of(23),
					"ONLY 7 STATES ALLOW FULLY REGULATED ONLINE CASINO GAMING"),
			new InDepthTokenSpec("map_sources", "Sources", List.of(23),
					"Source: USDA Farm Land Summary 2026,"));

	private InDepthTemplateTokens() {
	}
}
