import type { InDepthBriefState } from "../model/types";

/** A blank In-Depth brief — the initial state and the start-over reset value. */
export const EMPTY_IN_DEPTH_BRIEF: InDepthBriefState = {
    category: "",
    clientName: "",
    clientWebsite: "",
    guidanceNotes: "",
};

/**
 * Contract field names mapped to the labels the In-Depth builder shows, so a rejected
 * request names the input the user can actually see and fix.
 */
export const IN_DEPTH_FIELD_LABELS: Record<string, string> = {
    category: "Category",
    clientName: "Client",
    clientWebsite: "Client website",
    guidanceNotes: "Focus notes",
    selectedSlides: "Checked slides",
    slideNote: "Redraft guidance",
    slideNumber: "Slide",
    currentFields: "Deck fields",
    fields: "Deck fields",
};

/** Message shown when the In-Depth draft request fails without a usable response body. */
export const IN_DEPTH_DRAFT_FALLBACK_ERROR = "Drafting failed. Check the AI connection and try again.";

/** Message shown when an In-Depth slide redraft fails without a usable response body. */
export const IN_DEPTH_REDRAFT_FALLBACK_ERROR = "Redraft failed. Check the AI connection and try again.";

/** Message shown when In-Depth deck creation fails without a usable response body. */
export const IN_DEPTH_DECK_FALLBACK_ERROR = "Deck creation failed. Check Google access and try again.";

/** Message shown when the slide catalog cannot be loaded. */
export const IN_DEPTH_CATALOG_ERROR = "Could not load the slide catalog. Try refreshing the page.";

/** Static preview images of the master template, one per slide number. */
export const IN_DEPTH_SLIDE_PREVIEW_PATH = "/in-depth-slides";

/** Separator between a slide's topic and its "layout X" variant suffix in the catalog title. */
export const IN_DEPTH_TITLE_VARIANT_SEPARATOR = " — ";

/** Values longer than this are edited in a textarea rather than a single-line input. */
export const IN_DEPTH_LONG_VALUE_THRESHOLD = 60;
