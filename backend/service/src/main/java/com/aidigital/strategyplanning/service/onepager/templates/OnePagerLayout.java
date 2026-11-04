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
 *
 * <p>The current master wraps some modules in groups so the designer can move them as one.
 * Assembly ungroups those wrappers first (see {@code *_WRAPPER_GROUP_IDS}), so every id below
 * names a top-level shape once that has happened.
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

	/** Groups on the body slide that wrap the spotlight cards; ungrouped before assembly. */
	public static final List<Integer> BODY_WRAPPER_GROUP_IDS = List.of(226);

	/** Groups on the KPI slide that wrap the capability cards and the KPI tiles; ungrouped before assembly. */
	public static final List<Integer> KPI_WRAPPER_GROUP_IDS = List.of(310, 341);

	/** Group on the insight slide that wraps the first insight card's icon; ungrouped before assembly. */
	public static final List<Integer> INSIGHT_WRAPPER_GROUP_IDS = List.of(382);

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
			new OnePagerKpiSlot(348, 342, 343),
			new OnePagerKpiSlot(344, 345, 349),
			new OnePagerKpiSlot(346, 350, 351),
			new OnePagerKpiSlot(347, 352, 353));

	/** The 3 spotlight-card slots, left to right (body slide). */
	public static final List<OnePagerSpotlightCardSlot> SPOTLIGHT_CARD_SLOTS = List.of(
			new OnePagerSpotlightCardSlot(227, List.of(228, 229, 230), 231, 232, List.of(233, 234, 235), 236),
			new OnePagerSpotlightCardSlot(237, List.of(238, 253, 257, 261, 269, 273, 277, 281), 239, 240,
					List.of(241, 242, 243), 244),
			new OnePagerSpotlightCardSlot(245, List.of(246, 285, 286, 287, 288), 247, 248,
					List.of(249, 250, 251), 252));

	/**
	 * The 7 KPI-chip groups of spotlight card index 1, left to right, top to bottom. Each group has
	 * exactly 3 children in a fixed order: a background pill, a short value badge, and a label.
	 */
	public static final List<Integer> SPOTLIGHT_KPI_CHIP_GROUP_IDS = List.of(253, 257, 261, 269, 273, 277, 281);

	/** The 3 stage-name pills of spotlight card index 2, top to bottom. */
	public static final List<Integer> SPOTLIGHT_STAGE_PILL_IDS = List.of(285, 287, 288);

	/** Image placeholder above the image-and-text block's title (KPI slide). */
	public static final int IMAGE_TEXT_IMAGE_ID = 294;

	/** Section-title shape of the image-and-text block (KPI slide). */
	public static final int IMAGE_TEXT_LABEL_ID = 296;

	/** Paragraph shape of the image-and-text block (KPI slide). */
	public static final int IMAGE_TEXT_BODY_ID = 297;

	/** Second, full-width image placeholder below the image-and-text paragraph (KPI slide). */
	public static final int IMAGE_TEXT_SECONDARY_IMAGE_ID = 298;

	/** Accent bar of the key-takeaway callout (KPI slide). */
	public static final int TAKEAWAY_BAR_ID = 299;

	/** Text shape of the key-takeaway callout (KPI slide). */
	public static final int TAKEAWAY_TEXT_ID = 300;

	/** Full-width divider above the impact-stats row (KPI slide). */
	public static final int STATS_TOP_DIVIDER_ID = 302;

	/** The 3 impact-stat slots, left to right (KPI slide). */
	public static final List<OnePagerStatSlot> STAT_SLOTS = List.of(
			new OnePagerStatSlot(301, 307),
			new OnePagerStatSlot(303, 308),
			new OnePagerStatSlot(304, 309));

	/** Vertical divider lines between impact stats, left to right (KPI slide). */
	public static final List<Integer> STAT_DIVIDER_IDS = List.of(305, 306);

	/** The 3 capability-card slots, left to right (KPI slide). */
	public static final List<OnePagerCapabilityCardSlot> CAPABILITY_CARD_SLOTS = List.of(
			new OnePagerCapabilityCardSlot(311, 314, 315, 313, 312, 316, List.of(317, 318, 319), 320),
			new OnePagerCapabilityCardSlot(321, 324, 325, 323, 322, 326, List.of(327, 328, 329), 330),
			new OnePagerCapabilityCardSlot(331, 334, 335, 333, 332, 336, List.of(337, 338, 339), 340));

	/** Full-bleed photo placeholders above the insight-card row (insight slide), cloned as-is. */
	public static final List<Integer> PHOTO_IDS = List.of(361, 362);

	/** The 3 icon-and-insight card slots, left to right (insight slide). */
	public static final List<OnePagerInsightCardSlot> INSIGHT_CARD_SLOTS = List.of(
			new OnePagerInsightCardSlot(363, 366, 367, 368, 383, 376, 384),
			new OnePagerInsightCardSlot(364, 369, 370, 371, 372, 377, 381),
			new OnePagerInsightCardSlot(365, 379, 373, 374, 375, 378, 380));

	/** Static first-paragraph label always shown above the drafted insight sentence. */
	public static final String INSIGHT_STATIC_LABEL = "KEY INSIGHT";

	/** Full-bleed photo background of the milestone banner (insight slide). */
	public static final int MILESTONE_PHOTO_ID = 385;

	/** Dark overlay over the milestone banner's photo (insight slide). */
	public static final int MILESTONE_OVERLAY_ID = 386;

	/** Full-width headline above the milestone banner's numbered steps (insight slide). */
	public static final int MILESTONE_HEADLINE_ID = 390;

	/** The 3 milestone-banner step slots, left to right (insight slide). */
	public static final List<OnePagerMilestoneSlot> MILESTONE_SLOTS = List.of(
			new OnePagerMilestoneSlot(388, 389, 387, 391),
			new OnePagerMilestoneSlot(393, 394, 392, 395),
			new OnePagerMilestoneSlot(397, 398, 396, 399));

	private OnePagerLayout() {
	}
}
