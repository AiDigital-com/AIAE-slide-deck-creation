package com.aidigital.strategyplanning.service.onepager.templates;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerCapabilityCardSlot;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerInsightCardSlot;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpiSlot;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerMilestoneSlot;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSolutionSlot;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSpotlightCardSlot;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerStatSlot;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerTrackerSlot;

import java.util.List;

/**
 * Shape-id catalog of the One Page Generator master template ("Extended - One Pager Templates",
 * see {@code app.one-pager.template-presentation-id}).
 *
 * <p>The master is a component library, not a finished deck: slides 1-3 are alternate
 * header/footer shells ({@link com.aidigital.strategyplanning.service.onepager.models.OnePagerShell})
 * and slides 4-6 hold the reusable body modules. The Slides API has no cross-slide "duplicate
 * this shape" request, so assembly exports the master as PPTX, clones shape XML from the module
 * slides onto the chosen shell with Apache POI, and uploads the result as a new Slides file.
 *
 * <p>All shape ids were read from the master's OOXML and are stable only while the master keeps
 * its structure; re-derive them before relying on this catalog if the master is edited.
 */
public final class OnePagerLayout {

	/** Slide width in EMU; fixed, only the page height ever changes. */
	public static final long PAGE_WIDTH_EMU = 7_772_400L;

	/** The master's own authored page height in EMU, the starting point the page sizer grows or shrinks from. */
	public static final long MASTER_PAGE_HEIGHT_EMU = 13_716_000L;

	/** Shortest page allowed (9in), so a sparse page never leaves the footer awkwardly close to the title. */
	public static final long MIN_PAGE_HEIGHT_EMU = 8_229_600L;

	/** Tallest page allowed (24in); a page that grows without limit stops reading as "one page". */
	public static final long MAX_PAGE_HEIGHT_EMU = 21_945_600L;

	/** Vertical gap in EMU between stacked body modules. */
	public static final long SECTION_GAP_EMU = 250_000L;

	/** Gap in EMU between the header's lowest element and the first body module. */
	public static final long HEADER_GAP_EMU = 200_000L;

	/** 0-based index of the slide holding the tracker, solution cards, and spotlight cards. */
	public static final int BODY_SLIDE_INDEX = 3;

	/** 0-based index of the slide holding the KPI tiles, image + text, takeaway, stats, and capability cards. */
	public static final int KPI_SLIDE_INDEX = 4;

	/** 0-based index of the slide holding the photos, insight cards, and milestone banner. */
	public static final int INSIGHT_SLIDE_INDEX = 5;

	/** Tracker container background (body slide); resized to fit only the used slots. */
	public static final int TRACKER_CONTAINER_ID = 153;

	/** Tracker connecting line (body slide); resized to span only the used slots. */
	public static final int TRACKER_LINE_ID = 154;

	/** Small paragraph shape (body slide) whose style is reused for the KPI disclaimer. */
	public static final int DISCLAIMER_STYLE_SOURCE_ID = 156;

	/** Section label shape (body slide) reused above the tracker and the KPI tiles. */
	public static final int SECTION_LABEL_ID = 162;

	/** All 5 tracker slots, in step order. */
	public static final List<OnePagerTrackerSlot> TRACKER_SLOTS = List.of(
			new OnePagerTrackerSlot(157, 160, 161),
			new OnePagerTrackerSlot(163, 174, 175),
			new OnePagerTrackerSlot(166, 176, 177),
			new OnePagerTrackerSlot(169, 178, 179),
			new OnePagerTrackerSlot(172, 180, 181));

	/** All 5 core-solution card slots, top to bottom. */
	public static final List<OnePagerSolutionSlot> SOLUTION_SLOTS = List.of(
			new OnePagerSolutionSlot(182, 185, 183, 184),
			new OnePagerSolutionSlot(186, 189, 187, 188),
			new OnePagerSolutionSlot(190, 193, 191, 192),
			new OnePagerSolutionSlot(194, 197, 195, 196),
			new OnePagerSolutionSlot(198, 201, 199, 200));

	/** Section label paired with the core-solution card stack (body slide). */
	public static final int SOLUTIONS_SECTION_LABEL_ID = 202;

	/** Label of the optional companion visual in the master's right column (body slide). */
	public static final int SOLUTIONS_VISUAL_LABEL_ID = 203;

	/** The companion visual's two stacked image placeholders, top to bottom, cloned as-is. */
	public static final List<Integer> SOLUTIONS_VISUAL_IMAGE_IDS = List.of(204, 205);

	/** All 4 KPI tile slots (KPI slide), in order. */
	public static final List<OnePagerKpiSlot> KPI_SLOTS = List.of(
			new OnePagerKpiSlot(295, 296, 297),
			new OnePagerKpiSlot(298, 299, 300),
			new OnePagerKpiSlot(301, 302, 303),
			new OnePagerKpiSlot(304, 305, 306));

	/** The 3 spotlight-card slots, left to right (body slide). */
	public static final List<OnePagerSpotlightCardSlot> SPOTLIGHT_CARD_SLOTS = List.of(
			new OnePagerSpotlightCardSlot(226, List.of(227, 228, 229), 230, 231, List.of(232, 233, 234), 235),
			new OnePagerSpotlightCardSlot(236, List.of(237, 252, 256, 260, 268, 272, 276, 280), 238, 239,
					List.of(240, 241, 242), 243),
			new OnePagerSpotlightCardSlot(244, List.of(245, 284, 285, 286, 287), 246, 247,
					List.of(248, 249, 250), 251));

	/**
	 * The 7 KPI-chip groups of spotlight card index 1, left to right, top to bottom. Each group has
	 * exactly 3 children in a fixed order: a background pill, a short value badge, and a label.
	 */
	public static final List<Integer> SPOTLIGHT_KPI_CHIP_GROUP_IDS = List.of(252, 256, 260, 268, 272, 276, 280);

	/** The 3 stage-name pills of spotlight card index 2, top to bottom. */
	public static final List<Integer> SPOTLIGHT_STAGE_PILL_IDS = List.of(284, 286, 287);

	/** Image placeholder above the image-and-text block's title (KPI slide). */
	public static final int IMAGE_TEXT_IMAGE_ID = 293;

	/** Section-title shape of the image-and-text block (KPI slide). */
	public static final int IMAGE_TEXT_LABEL_ID = 307;

	/** Paragraph shape of the image-and-text block (KPI slide). */
	public static final int IMAGE_TEXT_BODY_ID = 308;

	/** Second, full-width image placeholder below the image-and-text paragraph (KPI slide). */
	public static final int IMAGE_TEXT_SECONDARY_IMAGE_ID = 309;

	/** Accent bar of the key-takeaway callout (KPI slide). */
	public static final int TAKEAWAY_BAR_ID = 310;

	/** Text shape of the key-takeaway callout (KPI slide). */
	public static final int TAKEAWAY_TEXT_ID = 311;

	/** Full-width divider above the impact-stats row (KPI slide). */
	public static final int STATS_TOP_DIVIDER_ID = 313;

	/** The 3 impact-stat slots, left to right (KPI slide). */
	public static final List<OnePagerStatSlot> STAT_SLOTS = List.of(
			new OnePagerStatSlot(312, 318),
			new OnePagerStatSlot(314, 319),
			new OnePagerStatSlot(315, 320));

	/** Vertical divider lines between impact stats, left to right (KPI slide). */
	public static final List<Integer> STAT_DIVIDER_IDS = List.of(316, 317);

	/** The 3 capability-card slots, left to right (KPI slide). */
	public static final List<OnePagerCapabilityCardSlot> CAPABILITY_CARD_SLOTS = List.of(
			new OnePagerCapabilityCardSlot(321, 324, 325, 323, 322, 326, List.of(327, 328, 329), 330),
			new OnePagerCapabilityCardSlot(331, 334, 335, 333, 332, 336, List.of(337, 338, 339), 340),
			new OnePagerCapabilityCardSlot(341, 344, 345, 343, 342, 346, List.of(347, 348, 349), 350));

	/** Full-bleed photo placeholders above the insight-card row (insight slide), cloned as-is. */
	public static final List<Integer> PHOTO_IDS = List.of(356, 357);

	/** The 3 icon-and-insight card slots, left to right (insight slide). */
	public static final List<OnePagerInsightCardSlot> INSIGHT_CARD_SLOTS = List.of(
			new OnePagerInsightCardSlot(358, 361, 362, 363, 364, 372, 378),
			new OnePagerInsightCardSlot(359, 365, 366, 367, 368, 373, 377),
			new OnePagerInsightCardSlot(360, 375, 369, 370, 371, 374, 376));

	/** Static first-paragraph label always shown above the drafted insight sentence. */
	public static final String INSIGHT_STATIC_LABEL = "KEY INSIGHT";

	/** Full-bleed photo background of the milestone banner (insight slide). */
	public static final int MILESTONE_PHOTO_ID = 379;

	/** Dark overlay over the milestone banner's photo (insight slide). */
	public static final int MILESTONE_OVERLAY_ID = 380;

	/** Full-width headline above the milestone banner's numbered steps (insight slide). */
	public static final int MILESTONE_HEADLINE_ID = 384;

	/** The 3 milestone-banner step slots, left to right (insight slide). */
	public static final List<OnePagerMilestoneSlot> MILESTONE_SLOTS = List.of(
			new OnePagerMilestoneSlot(382, 383, 381, 385),
			new OnePagerMilestoneSlot(387, 388, 386, 389),
			new OnePagerMilestoneSlot(391, 392, 390, 393));

	private OnePagerLayout() {
	}
}
