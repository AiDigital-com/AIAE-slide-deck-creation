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
