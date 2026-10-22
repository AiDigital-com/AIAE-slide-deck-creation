import { ONE_PAGER_TEMPLATE_KIND } from "../constants/form";
import type { RecentPage } from "./types";

/**
 * Keeps only the rows the One Page Generator owns. Every deck tool records into the same
 * category-analysis list, so the template kind is what tells a one-pager from a Standard or
 * In-Depth deck. The list already arrives newest first; order is preserved.
 */
export function selectRecentPages(rows: RecentPage[] | undefined): RecentPage[] {
    if (!rows) return [];
    return rows.filter((row) => row.templateKind === ONE_PAGER_TEMPLATE_KIND);
}
