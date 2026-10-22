import type { ModuleKey, ShellVariant } from "../model/types";

/** The master template's page width (EMU); fixed, only the height ever changes. */
export const PAGE_WIDTH_EMU = 7_772_400;

/**
 * The master's own authored page height (EMU), used only to derive each shell's footer height
 * (`DEFAULT_PAGE_HEIGHT_EMU - body.bottom`). Mirrors the backend's `MASTER_PAGE_HEIGHT_EMU`.
 */
export const DEFAULT_PAGE_HEIGHT_EMU = 13_716_000;

/** Shortest and tallest page the backend assembles (9in / 24in), so the preview clamps alike. */
export const MIN_PAGE_HEIGHT_EMU = 8_229_600;
export const MAX_PAGE_HEIGHT_EMU = 21_945_600;

/**
 * Usable body range (EMU) of each shell on the master's unmodified page: the header's reserved
 * floor (background art plus the two-line default title and copy, matching the backend header
 * composer) down to the footer band's top. `bottom` only derives the footer height now that the
 * real page grows and shrinks with content.
 */
export const SHELL_BODY_EMU: Record<ShellVariant, { top: number; bottom: number }> = {
    BLUE_HEADER: { top: 2_949_200, bottom: 12_748_000 },
    HERO_GRAPHIC: { top: 4_559_600, bottom: 12_748_000 },
    TOP_BAR: { top: 2_384_600, bottom: 12_696_825 },
};

/** Shared side margin of the standardized preview grid (roughly the smallest real inset). */
export const GRID_MARGIN_EMU = 300_000;

/** Gap between stacked rows and between the two halves of a half-width row. */
export const GRID_GAP_EMU = 120_000;

/** Full and half content widths of the preview grid. */
export const GRID_CONTENT_WIDTH_EMU = PAGE_WIDTH_EMU - GRID_MARGIN_EMU * 2;
export const GRID_HALF_WIDTH_EMU = (GRID_CONTENT_WIDTH_EMU - GRID_GAP_EMU) / 2;

/**
 * Modules whose real geometry is meaningfully narrower than the page (about half its width):
 * two of them pack into one row side by side instead of stretching to full width.
 */
export const HALF_WIDTH_KEYS: ReadonlySet<ModuleKey> = new Set<ModuleKey>(["SOLUTIONS", "SOLUTIONS_VISUAL", "PHOTO"]);

/**
 * Each module's rendered height (EMU) at its grid width, solved from the preview crop's true
 * aspect ratio so the image renders undistorted (verified against assembled files; see the
 * backend assembly constants for the real spacing SOLUTIONS and KPIS use).
 */
export const MODULE_HEIGHTS_EMU: Record<ModuleKey, number> = {
    TRACKER: 1_461_376,
    SOLUTIONS: 6_350_000,
    SOLUTIONS_VISUAL: 5_309_637,
    KPIS: 840_848,
    IMAGE_TEXT: 8_241_000,
    KEY_TAKEAWAY: 1_297_500,
    IMPACT_STATS: 1_162_650,
    CAPABILITY_CARDS: 3_317_235,
    SPOTLIGHT_CARDS: 3_344_804,
    INSIGHT_CARDS: 3_263_442,
    PHOTO: 4_168_772,
    MILESTONE_BANNER: 1_962_479,
};

/** Smallest height (percent of the page) a placed block renders at, so it stays grabbable. */
export const MIN_BLOCK_HEIGHT_PCT = 2;

/** Offset (percent of the page) of the first and last drop gap from the rows they border. */
export const GAP_INSET_PCT = 1;

/** Where the only drop gap sits when the canvas is empty. */
export const EMPTY_CANVAS_GAP_PCT = 50;
