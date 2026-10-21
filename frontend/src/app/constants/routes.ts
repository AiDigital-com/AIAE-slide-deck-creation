import {
    CASE_STUDY_PICKER_ROUTE,
    ONE_PAGE_CASE_STUDY_ROUTE,
    THREE_PAGE_CASE_STUDY_ROUTE,
} from "../../features/case-study-builder/constants/templates";
import {
    CATEGORY_ANALYSIS_PICKER_ROUTE,
    IN_DEPTH_CATEGORY_ANALYSIS_ROUTE,
    STANDARD_CATEGORY_ANALYSIS_ROUTE,
} from "../../features/category-analysis-builder/constants/templates";
import { TOOLS } from "../../features/strategy-toolkit/constants/tools";

/** The three Case Study routes: the picker and the two template builders behind it. */
export const CASE_STUDY_ROUTES = {
    picker: CASE_STUDY_PICKER_ROUTE,
    threePage: THREE_PAGE_CASE_STUDY_ROUTE,
    onePage: ONE_PAGE_CASE_STUDY_ROUTE,
} as const;

/** The three Category Analysis routes: the picker and the two template builders behind it. */
export const CATEGORY_ANALYSIS_ROUTES = {
    picker: CATEGORY_ANALYSIS_PICKER_ROUTE,
    standard: STANDARD_CATEGORY_ANALYSIS_ROUTE,
    inDepth: IN_DEPTH_CATEGORY_ANALYSIS_ROUTE,
} as const;

/** Route of the RnD request triage page, which the toolkit card list gates. */
export const RND_REQUEST_TRIAGE_ROUTE = "/rnd-request-triage";

/**
 * Whether the RnD request triage page is reachable. The toolkit card list is the single
 * source of truth for which internal tools are live, so the router follows it rather than
 * carrying a second copy of the decision.
 */
export const isRndRequestTriageAvailable =
    TOOLS.find((tool) => tool.route === RND_REQUEST_TRIAGE_ROUTE)?.available ?? false;
