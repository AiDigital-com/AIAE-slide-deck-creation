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
