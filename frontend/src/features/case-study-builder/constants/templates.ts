import type { CaseStudyTemplateOption } from "../model/types";

/** Route of the template picker shown before either Case Study flow. */
export const CASE_STUDY_PICKER_ROUTE = "/case-study-builder";

/** Route of the Three-Page Case Study builder (the original form-based flow). */
export const THREE_PAGE_CASE_STUDY_ROUTE = "/case-study-builder/three-page";

/** Route of the One-Page Case Study builder. */
export const ONE_PAGE_CASE_STUDY_ROUTE = "/case-study-builder/one-page";

/** The Case Study templates offered by the picker, in display order. */
export const CASE_STUDY_TEMPLATES: CaseStudyTemplateOption[] = [
    {
        key: "three-page",
        title: "Three-Page Case Study",
        description: "Draft a three-slide performance case study from uploaded reports or manual entry.",
        route: THREE_PAGE_CASE_STUDY_ROUTE,
    },
    {
        key: "one-page",
        title: "One-Page Case Study",
        description:
            "Upload campaign reports and get a single-slide case study: challenge, tactics, game plan, results, and key stats, filled from your files.",
        route: ONE_PAGE_CASE_STUDY_ROUTE,
    },
];
