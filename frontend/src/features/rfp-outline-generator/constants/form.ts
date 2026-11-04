import type { ApiFieldLabels } from "../../../shared/api/model/types";
import type { FormState } from "../model/types";

/** A blank RFP outline form — the initial state and the post-submit reset value. */
export const EMPTY_FORM: FormState = {
    title: "",
    clientName: "",
    industry: "",
    challenge: "",
    opportunity: "",
    solution: "",
    outcome: "",
    deckOutline: "",
    supplementaryNotes: "",
    sourceFileName: "",
};

/** Upload ceiling per draft request; mirrors app.rfp-outline.max-source-files on the backend. */
export const MAX_SOURCE_FILES = 10;

/** Shown when the auto-draft call fails without a usable error body. */
export const DRAFT_FALLBACK_ERROR = "Could not draft from the uploaded files. Please try again.";

/** Shown when saving or exporting the outline fails without a usable error body. */
export const CREATE_FALLBACK_ERROR = "Failed to export the RFP outline. Please try again.";

/** Contract field names mapped to the labels the form shows next to each input. */
export const RFP_FIELD_LABELS: ApiFieldLabels = {
    title: "Title",
    clientName: "Client Name",
    industry: "Industry",
    challenge: "Challenge",
    opportunity: "Opportunity",
    solution: "Solution",
    outcome: "Outcome",
    deckOutline: "Deck Outline",
    supplementaryNotes: "Supplementary notes",
    sourceFileName: "Source files",
    files: "Source files",
    notes: "Supplementary notes",
};
