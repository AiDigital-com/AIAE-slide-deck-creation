import type { BriefState, ModuleKey, ModuleOption, ShellOption, ShellVariant } from "../model/types";

/** Folder the module and shell previews are served from (also a public path on the backend). */
export const MODULE_PREVIEW_PATH = "/one-pager-modules";

/** Short label of every body module, used in the outline, tray, and remove buttons. */
export const MODULE_LABELS: Record<ModuleKey, string> = {
    TRACKER: "Process tracker",
    SOLUTIONS: "Core solutions",
    KPIS: "Performance signals",
    IMAGE_TEXT: "Image + text",
    KEY_TAKEAWAY: "Key takeaway",
    IMPACT_STATS: "Impact stats",
    CAPABILITY_CARDS: "Capability cards",
    SPOTLIGHT_CARDS: "Spotlight cards",
    INSIGHT_CARDS: "Insight cards",
    PHOTO: "Photo",
    SOLUTIONS_VISUAL: "Solutions visual",
    MILESTONE_BANNER: "Milestone banner",
};

/** The three header/footer shells, in master-template order. */
export const SHELLS: ShellOption[] = [
    {
        value: "BLUE_HEADER",
        label: "Shell 1 — blue header",
        thumbnail: `${MODULE_PREVIEW_PATH}/shell-1.jpg`,
        headerThumbnail: `${MODULE_PREVIEW_PATH}/shell-1-header.jpg`,
        footerThumbnail: `${MODULE_PREVIEW_PATH}/shell-1-footer.jpg`,
    },
    {
        value: "HERO_GRAPHIC",
        label: "Shell 2 — hero graphic",
        thumbnail: `${MODULE_PREVIEW_PATH}/shell-2.jpg`,
        headerThumbnail: `${MODULE_PREVIEW_PATH}/shell-2-header.jpg`,
        footerThumbnail: `${MODULE_PREVIEW_PATH}/shell-2-footer.jpg`,
    },
    {
        value: "TOP_BAR",
        label: "Shell 3 — top bar",
        thumbnail: `${MODULE_PREVIEW_PATH}/shell-3.jpg`,
        headerThumbnail: `${MODULE_PREVIEW_PATH}/shell-3-header.jpg`,
        footerThumbnail: `${MODULE_PREVIEW_PATH}/shell-3-footer.jpg`,
    },
];

/** Shell shown until the user picks one; its hero leaves the most body room. */
export const DEFAULT_SHELL: ShellVariant = "TOP_BAR";

/** Every pickable body module with its picker copy and thumbnail, in picker order. */
export const MODULE_OPTIONS: ModuleOption[] = [
    {
        key: "TRACKER",
        title: MODULE_LABELS.TRACKER,
        description: "A 5-step process tracker with a section label.",
        thumbnail: `${MODULE_PREVIEW_PATH}/tracker-5.jpg`,
    },
    {
        key: "SOLUTIONS",
        title: MODULE_LABELS.SOLUTIONS,
        description: "5 numbered solution cards, each with a title, value line, and body.",
        thumbnail: `${MODULE_PREVIEW_PATH}/solutions-5.jpg`,
    },
    {
        key: "SOLUTIONS_VISUAL",
        title: MODULE_LABELS.SOLUTIONS_VISUAL,
        description: "A companion visual beside the solution cards — needs Core solutions.",
        thumbnail: `${MODULE_PREVIEW_PATH}/solutions-visual.jpg`,
        requires: "SOLUTIONS",
    },
    {
        key: "KPIS",
        title: MODULE_LABELS.KPIS,
        description: "4 KPI tiles with a section label and a disclaimer.",
        thumbnail: `${MODULE_PREVIEW_PATH}/kpis-4.jpg`,
    },
    {
        key: "IMAGE_TEXT",
        title: MODULE_LABELS.IMAGE_TEXT,
        description: "An image paired with a section title and paragraph.",
        thumbnail: `${MODULE_PREVIEW_PATH}/image-text.jpg`,
    },
    {
        key: "KEY_TAKEAWAY",
        title: MODULE_LABELS.KEY_TAKEAWAY,
        description: "A single callout bar for one standout sentence.",
        thumbnail: `${MODULE_PREVIEW_PATH}/key-takeaway.jpg`,
    },
    {
        key: "IMPACT_STATS",
        title: MODULE_LABELS.IMPACT_STATS,
        description: "3 large percent/multiplier stats with descriptions.",
        thumbnail: `${MODULE_PREVIEW_PATH}/impact-stats.jpg`,
    },
    {
        key: "CAPABILITY_CARDS",
        title: MODULE_LABELS.CAPABILITY_CARDS,
        description: "3 cards, each with a title, description, checklist, and footer.",
        thumbnail: `${MODULE_PREVIEW_PATH}/capability-cards.jpg`,
    },
    {
        key: "SPOTLIGHT_CARDS",
        title: MODULE_LABELS.SPOTLIGHT_CARDS,
        description: "3 cards with different visuals: image, KPI chips, and a stage banner.",
        thumbnail: `${MODULE_PREVIEW_PATH}/preview-spotlight-cards.png`,
    },
    {
        key: "INSIGHT_CARDS",
        title: MODULE_LABELS.INSIGHT_CARDS,
        description: "3 icon-and-insight cards.",
        thumbnail: `${MODULE_PREVIEW_PATH}/insight-cards.jpg`,
    },
    {
        key: "PHOTO",
        title: MODULE_LABELS.PHOTO,
        description: "A standalone full-bleed photo placeholder.",
        thumbnail: `${MODULE_PREVIEW_PATH}/photo-banner.jpg`,
    },
    {
        key: "MILESTONE_BANNER",
        title: MODULE_LABELS.MILESTONE_BANNER,
        description: "A photo banner with a headline and 3 numbered steps.",
        thumbnail: `${MODULE_PREVIEW_PATH}/milestone-banner.jpg`,
    },
];

/**
 * Live-preview crops taken from the master template for every module (real pixels, not CSS
 * mockups). Solutions and KPIs repeat one cropped unit at the real assembly spacing because
 * assembly restretches those two from the master's native spacing; everything else is cloned as
 * a block, so the master's own crop already matches the output.
 */
export const MODULE_PREVIEWS: Record<ModuleKey, string> = {
    TRACKER: `${MODULE_PREVIEW_PATH}/preview-tracker-5.png`,
    SOLUTIONS: `${MODULE_PREVIEW_PATH}/preview-solution-card.png`,
    SOLUTIONS_VISUAL: `${MODULE_PREVIEW_PATH}/preview-solutions-visual.png`,
    KPIS: `${MODULE_PREVIEW_PATH}/preview-kpi-tile.png`,
    IMAGE_TEXT: `${MODULE_PREVIEW_PATH}/preview-image-text.png`,
    KEY_TAKEAWAY: `${MODULE_PREVIEW_PATH}/preview-key-takeaway.png`,
    IMPACT_STATS: `${MODULE_PREVIEW_PATH}/preview-impact-stats.png`,
    CAPABILITY_CARDS: `${MODULE_PREVIEW_PATH}/preview-capability-cards.png`,
    SPOTLIGHT_CARDS: `${MODULE_PREVIEW_PATH}/preview-spotlight-cards.png`,
    INSIGHT_CARDS: `${MODULE_PREVIEW_PATH}/preview-insight-cards.png`,
    PHOTO: `${MODULE_PREVIEW_PATH}/preview-photo.jpg`,
    MILESTONE_BANNER: `${MODULE_PREVIEW_PATH}/preview-milestone-banner.jpg`,
};

/** Solution cards the master provides; the preview repeats one card this many times. */
export const SOLUTION_CARD_COUNT = 5;

/** KPI tiles the master provides; the preview repeats one tile this many times. */
export const KPI_TILE_COUNT = 4;

/** The brief a fresh builder starts from: shell 3 with the two most-used modules. */
export const EMPTY_BRIEF: BriefState = {
    category: "",
    clientName: "",
    guidanceNotes: "",
    shellVariant: DEFAULT_SHELL,
    moduleOrder: ["SOLUTIONS", "KPIS"],
};
