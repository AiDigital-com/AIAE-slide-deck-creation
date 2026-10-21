/** Template kind recorded for Standard decks. */
const STANDARD_TEMPLATE_KIND = "STANDARD";

/** Template kind recorded for In-Depth decks. */
const IN_DEPTH_TEMPLATE_KIND = "IN_DEPTH";

/** The part of a list row the recent-analyses filters read. */
export interface KindedRow {
    templateKind?: string | null;
}

/**
 * Keeps the rows the Standard builder owns: STANDARD rows plus rows with no kind, which are the
 * manual submissions created before the column was used. In-Depth and One-Pager rows are dropped.
 * Order is preserved.
 */
export function selectStandardAnalyses<T extends KindedRow>(rows: T[] | undefined): T[] {
    if (!rows) return [];
    return rows.filter((row) => !row.templateKind || row.templateKind === STANDARD_TEMPLATE_KIND);
}

/** Keeps only the rows the In-Depth builder owns. Order is preserved. */
export function selectInDepthAnalyses<T extends KindedRow>(rows: T[] | undefined): T[] {
    if (!rows) return [];
    return rows.filter((row) => row.templateKind === IN_DEPTH_TEMPLATE_KIND);
}
