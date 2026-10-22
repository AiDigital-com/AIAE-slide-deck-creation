import type { components } from "../../../shared/api/generated/schema";

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

/** One Case Study template offered by the picker page. */
export interface CaseStudyTemplateOption {
    key: string;
    title: string;
    description: string;
    route: string;
}

/** One drafted field of the One-Page Case Study, with how it fits its box on the slide. */
export type OnePageField = components["schemas"]["OnePageCaseStudyFieldV1"];

/** One icon slot of the One-Page Case Study and the library icon chosen for it. */
export type OnePageIcon = components["schemas"]["OnePageCaseStudyIconV1"];

/** One icon of the library the One-Page reviewer can pick from. */
export type OnePageIconOption = components["schemas"]["OnePageCaseStudyIconOptionV1"];

/** Reviewed One-Page content posted to create the deck. */
export type CreateOnePageRequest = components["schemas"]["CreateOnePageCaseStudyRequestV1"];

/** State of the caller's Google Drive/Slides connection, shared with the Category Analysis flows. */
export type OnePageGoogleStatus = components["schemas"]["StandardConnectionsV1"]["googleStatus"];

/** The three stages of the One-Page builder: upload files, review the draft, show the deck. */
export type OnePageStep = "upload" | "review" | "done";

/** Drafted fields grouped under one template section, in slide order. */
export interface OnePageFieldSection {
    section: string;
    fields: OnePageField[];
}

/** Outcome of merging newly chosen files into the upload list. */
export interface MergeFilesResult<F> {
    files: F[];
    /** Files dropped because the list would exceed the maximum. */
    overflow: number;
    /** Names of files skipped because their type is not supported. */
    rejected: string[];
}
