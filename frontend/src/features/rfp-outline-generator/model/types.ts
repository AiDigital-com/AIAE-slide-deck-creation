/** Editable fields of the RFP outline form. */
export interface FormState {
    title: string;
    clientName: string;
    industry: string;
    challenge: string;
    opportunity: string;
    solution: string;
    outcome: string;
    deckOutline: string;
    supplementaryNotes: string;
    sourceFileName: string;
}

/** Client-side validation messages keyed by the form field they belong to. */
export type FieldErrors = Partial<Record<keyof FormState, string>>;
