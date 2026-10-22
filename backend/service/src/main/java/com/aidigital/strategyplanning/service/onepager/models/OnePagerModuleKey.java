package com.aidigital.strategyplanning.service.onepager.models;

/**
 * A body module of the One Page Generator page. The user arranges the modules top to bottom in
 * the picker; the page is assembled in that order. Mirrors the API's {@code OnePagerModuleKeyV1}.
 */
public enum OnePagerModuleKey {

	/** The 5-step process tracker. */
	TRACKER,

	/** The 5 core-solution cards. */
	SOLUTIONS,

	/** The 4 performance-signal KPI tiles. */
	KPIS,

	/** An image paired with a section title and paragraph. */
	IMAGE_TEXT,

	/** A single callout bar. */
	KEY_TAKEAWAY,

	/** 3 large percent/multiplier stats with descriptions. */
	IMPACT_STATS,

	/** 3 uniform cards with a title, description, checklist, and footer. */
	CAPABILITY_CARDS,

	/** 3 intentionally different cards: an image, a KPI-chip cluster, and 3 stage-name pills. */
	SPOTLIGHT_CARDS,

	/** 3 icon-and-insight cards. */
	INSIGHT_CARDS,

	/** A standalone full-bleed photo. */
	PHOTO,

	/** The optional companion visual beside the core-solution cards; rendered with SOLUTIONS only. */
	SOLUTIONS_VISUAL,

	/** A photo banner with a headline and 3 numbered steps. */
	MILESTONE_BANNER
}
