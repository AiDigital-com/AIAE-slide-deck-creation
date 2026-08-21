package com.aidigital.strategyplanning.service.categoryanalysis.templates;

import java.util.List;

/**
 * Canonical token registry for the Standard Category Analysis Google Slides template.
 * Each token maps a stable key to its slide, a review-UI label, and the sample text
 * currently present in the master template. The deck filler replaces BOTH the
 * {@code {{token}}} form and the sample literal, so the master deck works before and
 * after it is tokenized.
 */
public final class StandardTemplateTokens {

	/**
	 * One token definition of the Standard template.
	 *
	 * @param key         stable token key (e.g. "trends_headline")
	 * @param label       human-readable label for the review UI
	 * @param slideNumber slide the token appears on (1-5)
	 * @param sampleText  literal sample text currently in the master template
	 */
	public record TokenSpec(String key, String label, int slideNumber, String sampleText) {

	}

	/**
	 * Sample literal of the retired slide-2 chart title. The chart title box is deleted from
	 * every deck copy at creation time (per user feedback it crowded the graph area), so this
	 * literal exists only so the deck filler can locate and remove that shape.
	 */
	public static final String CHART_TITLE_SAMPLE =
			"MEDICARE ADVANTAGE SHARE OF ELIGIBLE MEDICARE BENEFICIARIES";

	/**
	 * Sample literal of the retired slide-3 section label. The label box is deleted from every
	 * deck copy at creation time (per user feedback it ended up buried behind the enlarged
	 * circle image), so this literal exists only so the deck filler can locate and remove it.
	 */
	public static final String DRIVERS_SECTION_LABEL_SAMPLE = "TRIP TYPES";

	/**
	 * Distinctive fragment of the slide-2 trend bullets text box. In the master template only
	 * bullets 1-2 start with a bold phrase while 3-4 are entirely regular weight, so replaced
	 * text inherits inconsistent styling. The deck filler locates the bullets box with this
	 * literal and bolds the whole box so all four bullets render consistently.
	 */
	public static final String TREND_BULLETS_SAMPLE =
			"Medicare Advantage is now a majority-share Medicare option";

	/**
	 * All tokens of the Standard Category Analysis template, ordered by slide.
	 */
	public static final List<TokenSpec> TOKENS = List.of(
			// Slide 1 — Title
			new TokenSpec("category_name", "Category name (title slide)", 1,
					"ONLINE CASINO/GAMING INDUSTRY"),

			// Slide 2 — Market trends + stats
			new TokenSpec("trends_headline", "Headline", 2,
					"AEP IS A HIGH-STAKES WINDOW IN A GROWING MEDICARE ADVANTAGE MARKET"),
			new TokenSpec("trends_intro", "Intro paragraph", 2,
					"Medicare and Medicaid audiences are navigating a short enrollment window, growing plan choice, "
							+ "and complex benefit decisions, making Spanish-first outreach critical during AEP."),
			new TokenSpec("trend_bullet_1", "Trend bullet 1", 2,
					"Medicare Advantage is now a majority-share Medicare option, making AEP a more competitive "
							+ "enrollment moment."),
			new TokenSpec("trend_bullet_2", "Trend bullet 2", 2,
					"Dual-eligible consumers represent a meaningful audience with distinct needs around " +
							"affordability, "
							+ "benefits, and eligibility clarity."),
			new TokenSpec("trend_bullet_3", "Trend bullet 3", 2,
					"As plan choice expands, the opportunity is not just to reach consumers, but to make enrollment "
							+ "information feel simple, relevant, and actionable."),
			new TokenSpec("trend_bullet_4", "Trend bullet 4", 2,
					"For UnitedHealthcare, Spanish-first messaging can help reduce friction during a short, "
							+ "high-consideration "
							+ "decision window."),
			new TokenSpec("stat_1_value", "Big stat 1 — value", 2, "54%"),
			new TokenSpec("stat_1_label", "Big stat 1 — label", 2,
					"of eligible Medicare beneficiaries are enrolled in Medicare Advantage plans in 2025"),
			new TokenSpec("stat_2_value", "Big stat 2 — value", 2, "11.9M"),
			new TokenSpec("stat_2_label", "Big stat 2 — label", 2,
					"people had both Medicare and Medicaid coverage in 2025"),
			new TokenSpec("trends_sources", "Sources", 2,
					"Source: KFF, Medicare Advantage in 2025; CMS, Medicare Open Enrollment Partner Resources."),

			// Slide 3 — Key drivers (4-quadrant)
			new TokenSpec("drivers_title", "Slide title", 3,
					"KEY DRIVERS OF REGIONAL TRAVEL"),
			new TokenSpec("drivers_headline", "Headline", 3,
					"TRAVELERS ARE SEEKING EASY, MEMORABLE ESCAPES CLOSE TO HOME"),
			new TokenSpec("drivers_intro", "Intro paragraph", 3,
					"Across life stages, travelers are prioritizing trips that feel simple to plan, flexible, "
							+ "and worth the spend, especially experiences that combine outdoor activity, "
							+ "quality time, "
							+ "and lower-crowd environments."),
			new TokenSpec("driver_1_title", "Driver 1 — title", 3, "Outdoor & Nature Appeal"),
			new TokenSpec("driver_1_text", "Driver 1 — text", 3,
					"Fresh air, scenic views, and simple escapes drive strong interest in open-air destinations."),
			new TokenSpec("driver_2_title", "Driver 2 — title", 3, "Value Without Sacrifice"),
			new TokenSpec("driver_2_text", "Driver 2 — text", 3,
					"Regional getaways offer the feeling of a real escape while helping travelers manage cost, "
							+ "time, and convenience."),
			new TokenSpec("driver_3_title", "Driver 3 — title", 3, "Shared Experiences"),
			new TokenSpec("driver_3_text", "Driver 3 — text", 3,
					"Travelers are looking for memory-making moments, from family time to couples trips "
							+ "and pet-friendly "
							+ "escapes."),
			new TokenSpec("driver_4_title", "Driver 4 — title", 3, "Easy, Flexible Planning"),
			new TokenSpec("driver_4_text", "Driver 4 — text", 3,
					"Shorter travel windows, driveability, and simple itineraries make regional trips easier "
							+ "to say yes to."),
			new TokenSpec("drivers_sources", "Sources", 3,
					"Source: NYU SPS / Family Travel Association 2025 Family Travel Survey; Expedia Group Unpack "
							+ "\u201925; U.S. Travel Association"),

			// Slide 4 — Media/channel opportunity
			new TokenSpec("channel_headline", "Headline", 4,
					"SPANISH-LANGUAGE STREAMING CAN TURN AWARENESS INTO ACTION"),
			new TokenSpec("channel_intro", "Intro paragraph", 4,
					"Hispanic audiences are shaping their own media experiences across streaming video, digital "
							+ "audio, and digital content."),
			new TokenSpec("channel_stat_1_value", "Channel stat 1 — value", 4, "56%"),
			new TokenSpec("channel_stat_1_label", "Channel stat 1 — label", 4,
					"of Hispanic viewers\u2019 total TV time is spent with streaming"),
			new TokenSpec("channel_stat_2_value", "Channel stat 2 — value", 4, "87%"),
			new TokenSpec("channel_stat_2_label", "Channel stat 2 — label", 4,
					"of U.S. Hispanic adults get news from digital devices"),
			new TokenSpec("channel_insight_1", "Insight paragraph 1", 4,
					"CTV can build repeated exposure in premium Spanish-language environments, "
							+ "helping UnitedHealthcare "
							+ "explain plan options before audiences are ready to take action."),
			new TokenSpec("channel_insight_2", "Insight paragraph 2", 4,
					"Streaming audio can reinforce Spanish-first messaging throughout the day, while display "
							+ "retargeting can bring interested users back."),
			new TokenSpec("channel_sources", "Sources", 4,
					"Source: KFF, Medicare Advantage in 2025; CMS, Medicare Open Enrollment Partner Resources."),

			// Slide 5 — Highlights & implications
			new TokenSpec("implications_headline", "Headline", 5,
					"WHAT THIS MEANS FOR ALBERTA BOOT\u2019S US EXPANSION"),
			new TokenSpec("highlight_1_title", "Highlight 1 — title", 5,
					"Functional Demand Anchors the Category"),
			new TokenSpec("highlight_1_text", "Highlight 1 — text", 5,
					"Work-driven industries sustain consistent boot purchases year-round, creating a stable demand "
							+ "base beyond fashion cycles."),
			new TokenSpec("highlight_2_title", "Highlight 2 — title", 5,
					"Social Discovery Fuels Awareness"),
			new TokenSpec("highlight_2_text", "Highlight 2 — text", 5,
					"Boot demand is regionally concentrated, making geo-targeted media in high-index and expansion "
							+ "markets more efficient."),
			new TokenSpec("highlight_3_title", "Highlight 3 — title", 5,
					"Western Identity Has Broadened"),
			new TokenSpec("highlight_3_text", "Highlight 3 — text", 5,
					"Western aesthetics now extend beyond ranch-driven consumers into lifestyle "
							+ "and fashion environments."),
			new TokenSpec("highlight_4_title", "Highlight 4 — title", 5,
					"Premium Craft Drives Trade-Up"),
			new TokenSpec("highlight_4_text", "Highlight 4 — text", 5,
					"Consumers increasingly value durability, full-grain leather, and long-term wear over fast "
							+ "fashion alternatives."),
			new TokenSpec("highlight_5_title", "Highlight 5 — title", 5,
					"Retail Expansion Signals Confidence"),
			new TokenSpec("highlight_5_text", "Highlight 5 — text", 5,
					"Specialty boot retailers continue opening stores nationwide, reinforcing sustained category "
							+ "growth."),
			new TokenSpec("implication_1_title", "Implication 1 — title", 5,
					"Lead with Modern Heritage"),
			new TokenSpec("implication_1_text", "Implication 1 — text", 5,
					"Position Alberta Boot as authentic yet versatile, appealing to both core western and lifestyle "
							+ "audiences."),
			new TokenSpec("implication_2_title", "Implication 2 — title", 5,
					"Elevate Craft Through Video"),
			new TokenSpec("implication_2_text", "Implication 2 — text", 5,
					"Leverage CTV, YouTube, and premium placements to showcase craftsmanship before consumers "
							+ "enter search."),
			new TokenSpec("implication_3_title", "Implication 3 — title", 5,
					"Activate Around Seasonal Peaks"),
			new TokenSpec("implication_3_text", "Implication 3 — text", 5,
					"Flight media strategically into Q3 and Q4 when intent accelerates and retail traffic spikes."));

	private StandardTemplateTokens() {
	}
}
