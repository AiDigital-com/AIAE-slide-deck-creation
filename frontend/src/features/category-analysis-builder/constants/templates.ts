import type { TemplateOption } from "../model/types";

/** Route of the template picker shown before either Category Analysis flow. */
export const CATEGORY_ANALYSIS_PICKER_ROUTE = "/category-analysis-builder";

/** Route of the Standard (5-slide) Category Analysis builder. */
export const STANDARD_CATEGORY_ANALYSIS_ROUTE = "/category-analysis-builder/standard";

/** Route of the In-Depth (24-slide layout library) Category Analysis builder. */
export const IN_DEPTH_CATEGORY_ANALYSIS_ROUTE = "/category-analysis-builder/in-depth";

/** The Category Analysis templates offered by the picker, in display order. */
export const CATEGORY_ANALYSIS_TEMPLATES: TemplateOption[] = [
    {
        key: "standard",
        title: "Standard Category Analysis",
        description:
            "A fast, 5-slide category analysis: market trends, key drivers, channel opportunity, and highlights & implications.",
        route: STANDARD_CATEGORY_ANALYSIS_ROUTE,
    },
    {
        key: "in-depth",
        title: "In-Depth Category Analysis",
        description:
            "Pick the slides you want from a 24-slide layout library — market trend charts, purchase drivers, channel opportunity, decision journeys, and more — then AI-drafts just the ones you checked, grounded in live web search.",
        route: IN_DEPTH_CATEGORY_ANALYSIS_ROUTE,
    },
];
