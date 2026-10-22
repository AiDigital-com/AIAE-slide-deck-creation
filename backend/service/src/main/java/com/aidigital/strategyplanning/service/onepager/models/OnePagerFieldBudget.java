package com.aidigital.strategyplanning.service.onepager.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Character budget of every drafted one-pager field, matching the fixed-size template box it is
 * written into. The prompt composer tells the model each budget and the parser caps what comes
 * back, so an over-length draft is cut at a word boundary rather than overflowing a box that
 * never resizes.
 */
@Getter
@RequiredArgsConstructor
public enum OnePagerFieldBudget {

	TOPIC_PILL(20),
	/** The title box grows with the number of lines, never with a line that wraps. */
	TITLE_LINE(28),
	SUPPORTING_COPY(60),
	SECTION_LABEL(40),
	TRACKER_STEP(16),
	SOLUTION_TITLE(46),
	SOLUTION_VALUE_LINE(75),
	SOLUTION_BODY(220),
	/** The KPI tile never shrinks its font to fit a longer value. */
	KPI_VALUE(6),
	KPI_LABEL(40),
	KPI_DISCLAIMER(200),
	SOLUTIONS_VISUAL_LABEL(40),
	IMAGE_TEXT_LABEL(40),
	IMAGE_TEXT_BODY(240),
	KEY_TAKEAWAY(160),
	STAT_VALUE(8),
	STAT_DESCRIPTION(40),
	CAPABILITY_TITLE(45),
	CAPABILITY_DESCRIPTION(120),
	CAPABILITY_BULLET(45),
	CAPABILITY_FOOTER(90),
	SPOTLIGHT_TITLE(45),
	SPOTLIGHT_DESCRIPTION(120),
	SPOTLIGHT_BULLET(45),
	SPOTLIGHT_STAGE_NAME(16),
	INSIGHT_TITLE(30),
	INSIGHT_TEXT(110),
	MILESTONE_HEADLINE(90),
	MILESTONE_TITLE(40),
	MILESTONE_SUBTEXT(90);

	/** Maximum characters the field's box holds. */
	private final int maxChars;
}
