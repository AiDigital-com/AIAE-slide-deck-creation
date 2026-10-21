import { describe, expect, it } from "vitest";

import { CANVAS_ZONE_ID, GAP_ID_PREFIX, OVERFLOW_ZONE_ID } from "../constants/dnd";
import {
    DEFAULT_PAGE_HEIGHT_EMU,
    GRID_CONTENT_WIDTH_EMU,
    GRID_GAP_EMU,
    GRID_HALF_WIDTH_EMU,
    GRID_MARGIN_EMU,
    MAX_PAGE_HEIGHT_EMU,
    MIN_PAGE_HEIGHT_EMU,
    MODULE_HEIGHTS_EMU,
    SHELL_BODY_EMU,
} from "../constants/layout";
import { EMPTY_BRIEF } from "../constants/modules";
import {
    computeGaps,
    computeLayout,
    dropInsertIndex,
    groupRows,
    moduleHeightEmu,
    recommendedVisualSlot,
    stackModulesRaw,
} from "./layout";
import type { ModuleKey, Placement } from "./types";

/** A placement at the given top and height, full width. */
function placed(key: ModuleKey, topPct: number, heightPct: number, leftPct = 0): Placement {
    return { key, leftPct, widthPct: 50, topPct, heightPct, fits: true };
}

describe("stackModulesRaw", () => {
    it("should pack adjacent half-width modules into one row and stack the rest full width test", () => {
        // Given: a full-width tracker, then solutions beside its visual, then a lone photo
        const order: ModuleKey[] = ["TRACKER", "SOLUTIONS", "SOLUTIONS_VISUAL", "PHOTO"];

        // When: the modules are stacked from a body top of 1000
        const { raw, contentBottom } = stackModulesRaw(order, 1000);

        // Then: the tracker spans the content width, the pair shares one row sized to the taller
        // module, the photo takes the left column alone, and the bottom is the photo's bottom
        const rowHeight = Math.max(MODULE_HEIGHTS_EMU.SOLUTIONS, MODULE_HEIGHTS_EMU.SOLUTIONS_VISUAL);
        const pairTop = 1000 + MODULE_HEIGHTS_EMU.TRACKER + GRID_GAP_EMU;
        expect(raw.map((p) => p.key)).toEqual(["TRACKER", "SOLUTIONS", "SOLUTIONS_VISUAL", "PHOTO"]);
        expect(raw[0]).toMatchObject({ leftEmu: GRID_MARGIN_EMU, widthEmu: GRID_CONTENT_WIDTH_EMU, topEmu: 1000 });
        expect(raw[1]).toMatchObject({ leftEmu: GRID_MARGIN_EMU, widthEmu: GRID_HALF_WIDTH_EMU, topEmu: pairTop, heightEmu: rowHeight });
        expect(raw[2]).toMatchObject({ leftEmu: GRID_MARGIN_EMU + GRID_HALF_WIDTH_EMU + GRID_GAP_EMU, topEmu: pairTop, heightEmu: rowHeight });
        expect(raw[3]).toMatchObject({ leftEmu: GRID_MARGIN_EMU, widthEmu: GRID_HALF_WIDTH_EMU, topEmu: pairTop + rowHeight + GRID_GAP_EMU });
        expect(contentBottom).toBe(raw[3].topEmu + MODULE_HEIGHTS_EMU.PHOTO);
    });

    it("should skip the solutions visual when the solution cards are absent test", () => {
        // Given-When-Then: the visual has no height on its own and is left out of the stack
        expect(moduleHeightEmu("SOLUTIONS_VISUAL", ["KPIS"])).toBeNull();
        expect(moduleHeightEmu("SOLUTIONS_VISUAL", ["SOLUTIONS"])).toBe(MODULE_HEIGHTS_EMU.SOLUTIONS_VISUAL);
        expect(stackModulesRaw(["SOLUTIONS_VISUAL", "KPIS"], 0).raw.map((p) => p.key)).toEqual(["KPIS"]);
        expect(stackModulesRaw([], 500)).toEqual({ raw: [], contentBottom: 500 });
    });
});

describe("computeLayout", () => {
    it("should size the page to the content plus the shell's footer and clamp at both ends test", () => {
        // Given: an empty page, a modest page, and a page with every module twice over
        const empty = { ...EMPTY_BRIEF, moduleOrder: [] as ModuleKey[] };
        const modest = { ...EMPTY_BRIEF, shellVariant: "BLUE_HEADER" as const, moduleOrder: ["IMAGE_TEXT"] as ModuleKey[] };
        const everything = { ...EMPTY_BRIEF, moduleOrder: ["IMAGE_TEXT", "IMAGE_TEXT", "IMAGE_TEXT"] as ModuleKey[] };

        // When: each layout is computed
        const emptyLayout = computeLayout(empty);
        const modestLayout = computeLayout(modest);
        const tallLayout = computeLayout(everything);

        // Then: the empty page sits at the floor, the modest page is content plus footer, and the
        // tall page hits the ceiling with its lowest module in overflow
        const footer = DEFAULT_PAGE_HEIGHT_EMU - SHELL_BODY_EMU.BLUE_HEADER.bottom;
        expect(emptyLayout).toEqual({ placements: [], pageHeightEmu: MIN_PAGE_HEIGHT_EMU });
        expect(modestLayout.pageHeightEmu).toBe(SHELL_BODY_EMU.BLUE_HEADER.top + MODULE_HEIGHTS_EMU.IMAGE_TEXT + footer);
        expect(modestLayout.placements[0].fits).toBe(true);
        expect(modestLayout.placements[0].topPct).toBeCloseTo((SHELL_BODY_EMU.BLUE_HEADER.top / modestLayout.pageHeightEmu) * 100);
        expect(tallLayout.pageHeightEmu).toBe(MAX_PAGE_HEIGHT_EMU);
        expect(tallLayout.placements.map((p) => p.fits)).toEqual([true, true, false]);
    });
});

describe("rows and gaps", () => {
    it("should group side-by-side placements and put a gap at every row boundary test", () => {
        // Given: a full-width row, a paired row, and another full-width row
        const fitting = [placed("TRACKER", 10, 10), placed("SOLUTIONS", 22, 30), placed("SOLUTIONS_VISUAL", 22, 30, 50), placed("KPIS", 54, 6)];

        // When: rows and gaps are derived
        const rows = groupRows(fitting);
        const gaps = computeGaps(rows);

        // Then: three rows, four gaps: above the first, between rows, below the last
        expect(rows.map((row) => row.map((p) => p.key))).toEqual([["TRACKER"], ["SOLUTIONS", "SOLUTIONS_VISUAL"], ["KPIS"]]);
        expect(gaps.map((g) => g.id)).toEqual([`${GAP_ID_PREFIX}0`, `${GAP_ID_PREFIX}1`, `${GAP_ID_PREFIX}2`, `${GAP_ID_PREFIX}3`]);
        expect(gaps.map((g) => g.topPct)).toEqual([9, 21, 53, 61]);
        expect(computeGaps([])).toEqual([{ id: `${GAP_ID_PREFIX}0`, topPct: 50 }]);
    });
});

describe("dropInsertIndex", () => {
    it("should resolve every drop target to a position in the order without the dragged module test", () => {
        // Given: four modules, three fitting in two rows
        const order: ModuleKey[] = ["TRACKER", "SOLUTIONS", "KPIS", "PHOTO"];
        const fitting = [placed("TRACKER", 10, 10), placed("SOLUTIONS", 22, 30), placed("KPIS", 54, 6)];
        const rows = groupRows(fitting);

        // When-Then: gaps land at row boundaries, zones append, modules insert before, unknown is null
        expect(dropInsertIndex(`${GAP_ID_PREFIX}0`, order, "PHOTO", rows, fitting)).toBe(0);
        expect(dropInsertIndex(`${GAP_ID_PREFIX}2`, order, "PHOTO", rows, fitting)).toBe(2);
        expect(dropInsertIndex(`${GAP_ID_PREFIX}3`, order, "TRACKER", rows, fitting)).toBe(2);
        expect(dropInsertIndex(CANVAS_ZONE_ID, order, "PHOTO", rows, fitting)).toBe(3);
        expect(dropInsertIndex(CANVAS_ZONE_ID, order, "PHOTO", [], [])).toBe(0);
        expect(dropInsertIndex(OVERFLOW_ZONE_ID, order, "TRACKER", rows, fitting)).toBe(3);
        expect(dropInsertIndex("SOLUTIONS", order, "PHOTO", rows, fitting)).toBe(1);
        expect(dropInsertIndex("MILESTONE_BANNER", order, "PHOTO", rows, fitting)).toBeNull();
    });
});

describe("recommendedVisualSlot", () => {
    it("should offer the slot beside placed solutions only while the visual is absent test", () => {
        // Given: placed solutions
        const solutions = placed("SOLUTIONS", 20, 40);

        // When-Then: the slot mirrors the cards in the right half, and disappears with the visual
        expect(recommendedVisualSlot([solutions], ["SOLUTIONS"])).toEqual({
            leftPct: 50 + (GRID_GAP_EMU / 7_772_400) * 100,
            widthPct: 50,
            topPct: 20,
            heightPct: 40,
        });
        expect(recommendedVisualSlot([solutions], ["SOLUTIONS", "SOLUTIONS_VISUAL"])).toBeNull();
        expect(recommendedVisualSlot([placed("KPIS", 1, 1)], ["KPIS"])).toBeNull();
    });
});
