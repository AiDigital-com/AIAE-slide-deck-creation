import type { BriefState } from "../model/types";

/** A blank deck brief — the initial state and the start-over reset value. */
export const EMPTY_BRIEF: BriefState = {
    category: "",
    clientName: "",
    clientWebsite: "",
    storyTheme: "",
    guidanceNotes: "",
};

/** Review-step headings for the five Standard deck slides. */
export const SLIDE_TITLES: Record<number, string> = {
    1: "Slide 1 — Title",
    2: "Slide 2 — Market Trends & Stats",
    3: "Slide 3 — Key Drivers",
    4: "Slide 4 — Media Opportunity",
    5: "Slide 5 — Highlights & Implications",
};

/**
 * Contract field names mapped to the labels this builder shows, so a rejected
 * request names the input the user can actually see and fix.
 */
export const BRIEF_FIELD_LABELS: Record<string, string> = {
    category: "Category",
    clientName: "Client",
    clientWebsite: "Client website",
    storyTheme: "Story theme",
    guidanceNotes: "Focus notes",
    slideNote: "Redraft guidance",
    slideNumber: "Slide",
    currentFields: "Deck fields",
    fields: "Deck fields",
};

/** Message shown when a draft request fails without a usable response body. */
export const DRAFT_FALLBACK_ERROR = "Drafting failed. Check the AI connection and try again.";

/** Message shown when a slide redraft fails without a usable response body. */
export const REDRAFT_FALLBACK_ERROR = "Redraft failed. Check the AI connection and try again.";

/** Message shown when deck creation fails without a usable response body. */
export const DECK_FALLBACK_ERROR = "Deck creation failed. Check Google access and try again.";
