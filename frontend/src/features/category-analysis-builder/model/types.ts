import type { components } from "../../../shared/api/generated/schema";

/** One AI-drafted field of the Standard deck, keyed by its template placeholder. */
export type StandardDraftField = components["schemas"]["StandardDraftFieldV1"];

/** How closely the draft matched the requested category and client. */
export type DraftAlignment = components["schemas"]["DraftAlignmentV1"];

/** State of the caller's Google Drive/Slides connection. */
export type GoogleStatus = components["schemas"]["StandardConnectionsV1"]["googleStatus"];

/** The three stages of the builder: collect the brief, review the draft, show the deck. */
export type Step = "brief" | "review" | "done";

/** Editable fields of the deck brief. */
export interface BriefState {
    category: string;
    clientName: string;
    clientWebsite: string;
    storyTheme: string;
    guidanceNotes: string;
}

/** Client-side validation messages keyed by the brief field they belong to. */
export type BriefFieldErrors = Partial<Record<keyof BriefState, string>>;

/** One selectable slide of the In-Depth master template's "check to include" preview. */
export type InDepthSlide = components["schemas"]["InDepthSlideV1"];

/** One AI-drafted field of the In-Depth deck, shared by every checked slide that shows it. */
export type InDepthDraftField = components["schemas"]["InDepthDraftFieldV1"];

/** The three stages of the In-Depth builder: pick slides and brief, review the draft, show the deck. */
export type InDepthStep = "setup" | "review" | "done";

/** Editable fields of the In-Depth deck brief (no story theme: the checked slides shape the arc). */
export interface InDepthBriefState {
    category: string;
    clientName: string;
    clientWebsite: string;
    guidanceNotes: string;
}

/** Client-side validation messages keyed by the In-Depth brief field they belong to. */
export type InDepthBriefFieldErrors = Partial<Record<keyof InDepthBriefState, string>>;

/** One Category Analysis template offered by the picker page. */
export interface TemplateOption {
    key: string;
    title: string;
    description: string;
    route: string;
}
