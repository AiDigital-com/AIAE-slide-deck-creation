/** Editable fields of the case study form. */
export interface FormState {
    title: string;
    clientName: string;
    industry: string;
    challenge: string;
    solution: string;
    results: string;
    keyMetrics: string;
    timeline: string;
    testimonial: string;
    sourceFileName: string;
}

/** Client-side validation messages keyed by the form field they belong to. */
export type FieldErrors = Partial<Record<keyof FormState, string>>;
