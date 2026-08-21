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
