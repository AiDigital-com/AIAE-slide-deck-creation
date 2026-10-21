import { describe, expect, it } from "vitest";

import { selectRecentPages } from "./recentPages";
import type { RecentPage } from "./types";

/** A list row with the given kind and otherwise fixed values. */
function row(id: number, templateKind: string | null | undefined): RecentPage {
    return {
        id,
        title: `Row ${id}`,
        category: "Automotive",
        slidesUrl: `https://docs.google.com/presentation/d/${id}/edit`,
        templateKind,
        status: "SUBMITTED",
        createdAt: "2026-10-21T09:00:00Z",
    };
}

describe("selectRecentPages", () => {
    it("should keep only ONE_PAGER rows in their original order test", () => {
        // Given: a mixed list as the shared category-analyses endpoint returns it
        const rows = [row(5, "ONE_PAGER"), row(4, "STANDARD"), row(3, "IN_DEPTH"), row(2, "ONE_PAGER"), row(1, null)];

        // When: the one-pager rows are selected
        const pages = selectRecentPages(rows);

        // Then: only the one-pagers remain, newest first as received
        expect(pages.map((page) => page.id)).toEqual([5, 2]);
    });

    it("should treat a missing kind as not a one-pager test", () => {
        // Given: a manual submission row with no kind at all
        const rows = [row(1, undefined)];

        // When-Then: nothing is selected
        expect(selectRecentPages(rows)).toEqual([]);
    });

    it("should return an empty list before the query has data test", () => {
        // Given: no data yet
        // When-Then: an empty list, so the component can render without guards
        expect(selectRecentPages(undefined)).toEqual([]);
    });
});
