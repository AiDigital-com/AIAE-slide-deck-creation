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

/**
 * File extensions the draft endpoint accepts; mirrors the backend's text extraction (PDF, Word,
 * PowerPoint, Excel, CSV, text/Markdown) plus the screenshot formats it forwards to the AI as images.
 */
export const ACCEPTED_SOURCE_FILE_EXTENSIONS =
    ".pdf,.docx,.doc,.pptx,.ppt,.xlsx,.xls,.csv,.txt,.md,.png,.jpg,.jpeg,.webp";

/** Human-readable summary of the accepted upload kinds for the Auto-Draft helper text. */
export const SOURCE_FILE_KINDS_LABEL = "PDF, Word, PowerPoint, Excel, CSV, text/Markdown, screenshots";
