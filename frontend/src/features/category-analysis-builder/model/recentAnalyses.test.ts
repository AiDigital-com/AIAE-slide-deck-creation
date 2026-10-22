import { describe, expect, it } from "vitest";

import { selectInDepthAnalyses, selectStandardAnalyses } from "./recentAnalyses";

/** A list row with the given kind. */
function row(id: number, templateKind: string | null | undefined) {
    return { id, templateKind };
}

const MIXED = [row(6, "ONE_PAGER"), row(5, "STANDARD"), row(4, "IN_DEPTH"), row(3, null), row(2, undefined), row(1, "IN_DEPTH")];

describe("selectStandardAnalyses", () => {
    it("should keep STANDARD and legacy rows only test", () => {
        // Given: a mixed list as the shared endpoint returns it
        // When: the Standard rows are selected
        const ids = selectStandardAnalyses(MIXED).map((item) => item.id);

        // Then: STANDARD plus null/missing kinds remain in order
        expect(ids).toEqual([5, 3, 2]);
    });

    it("should return an empty list before data arrives test", () => {
        // Given: no data yet
        // When-Then: an empty list
        expect(selectStandardAnalyses(undefined)).toEqual([]);
    });
});

describe("selectInDepthAnalyses", () => {
    it("should keep IN_DEPTH rows only test", () => {
        // Given: a mixed list
        // When: the In-Depth rows are selected
        const ids = selectInDepthAnalyses(MIXED).map((item) => item.id);

        // Then: legacy and other kinds are excluded
        expect(ids).toEqual([4, 1]);
    });

    it("should return an empty list before data arrives test", () => {
        // Given: no data yet
        // When-Then: an empty list
        expect(selectInDepthAnalyses(undefined)).toEqual([]);
    });
});
