import type { FormState } from "../model/types";

/** A blank case study form — the initial state and the post-submit reset value. */
export const EMPTY_FORM: FormState = {
    title: "",
    clientName: "",
    industry: "",
    challenge: "",
    solution: "",
    results: "",
    keyMetrics: "",
    timeline: "",
    testimonial: "",
    sourceFileName: "",
};

/** Upload ceiling per draft request; mirrors app.case-study.max-source-files on the backend. */
export const MAX_SOURCE_FILES = 10;
