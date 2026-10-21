import { CANVAS_ZONE_ID, GAP_ID_PREFIX, OVERFLOW_ZONE_ID } from "../constants/dnd";
import {
    DEFAULT_PAGE_HEIGHT_EMU,
    EMPTY_CANVAS_GAP_PCT,
    GAP_INSET_PCT,
    GRID_CONTENT_WIDTH_EMU,
    GRID_GAP_EMU,
    GRID_HALF_WIDTH_EMU,
    GRID_MARGIN_EMU,
    HALF_WIDTH_KEYS,
    MAX_PAGE_HEIGHT_EMU,
    MIN_BLOCK_HEIGHT_PCT,
    MIN_PAGE_HEIGHT_EMU,
    MODULE_HEIGHTS_EMU,
    PAGE_WIDTH_EMU,
    SHELL_BODY_EMU,
} from "../constants/layout";
import type { BriefState, DropGap, ModuleKey, Placement, PreviewLayout, RawPlacement, SlotBox } from "./types";

/**
 * A module's height in the preview, or null when it does not render: the solutions visual only
 * renders beside the solution cards, exactly as the backend assembles it.
 */
export function moduleHeightEmu(key: ModuleKey, order: ModuleKey[]): number | null {
    if (key === "SOLUTIONS_VISUAL" && !order.includes("SOLUTIONS")) return null;
    return MODULE_HEIGHTS_EMU[key];
}

/**
 * Stacks the modules top to bottom from the shell's body start on a two-column grid, in raw EMU.
 * Every module is full width or half width, never anything in between. Two half-width modules
 * back to back in the stacking order pack into one row sized to the taller of the two; a lone
 * half-width module takes the left column. Returns the placements and the last module's bottom.
 */
export function stackModulesRaw(order: ModuleKey[], bodyTop: number): { raw: RawPlacement[]; contentBottom: number } {
    let cursor = bodyTop;
    let contentBottom = bodyTop;
    const raw: RawPlacement[] = [];
    let pendingHalf: { key: ModuleKey; heightEmu: number } | null = null;

    const flushPendingHalf = () => {
        if (!pendingHalf) return;
        raw.push({
            key: pendingHalf.key,
            leftEmu: GRID_MARGIN_EMU,
            widthEmu: GRID_HALF_WIDTH_EMU,
            topEmu: cursor,
            heightEmu: pendingHalf.heightEmu,
        });
        contentBottom = cursor + pendingHalf.heightEmu;
        cursor = contentBottom + GRID_GAP_EMU;
        pendingHalf = null;
    };

    for (const key of order) {
        const heightEmu = moduleHeightEmu(key, order);
        if (heightEmu === null) continue;
        if (HALF_WIDTH_KEYS.has(key)) {
            if (pendingHalf) {
                const rowHeight = Math.max(pendingHalf.heightEmu, heightEmu);
                raw.push({ key: pendingHalf.key, leftEmu: GRID_MARGIN_EMU, widthEmu: GRID_HALF_WIDTH_EMU, topEmu: cursor, heightEmu: rowHeight });
                raw.push({
                    key,
                    leftEmu: GRID_MARGIN_EMU + GRID_HALF_WIDTH_EMU + GRID_GAP_EMU,
                    widthEmu: GRID_HALF_WIDTH_EMU,
                    topEmu: cursor,
                    heightEmu: rowHeight,
                });
                contentBottom = cursor + rowHeight;
                cursor = contentBottom + GRID_GAP_EMU;
                pendingHalf = null;
            } else {
                pendingHalf = { key, heightEmu };
            }
        } else {
            flushPendingHalf();
            raw.push({ key, leftEmu: GRID_MARGIN_EMU, widthEmu: GRID_CONTENT_WIDTH_EMU, topEmu: cursor, heightEmu });
            contentBottom = cursor + heightEmu;
            cursor = contentBottom + GRID_GAP_EMU;
        }
    }
    flushPendingHalf();
    return { raw, contentBottom };
}

/**
 * Stacks the brief's modules and sizes the page around them the way the backend does: content
 * height plus the shell's footer height, clamped to the same floor and ceiling. Placements are
 * then expressed as percentages of that height. A module lands in the overflow tray only when the
 * stack hits the ceiling, the same rare case the backend warns about.
 */
export function computeLayout(brief: BriefState): PreviewLayout {
    const body = SHELL_BODY_EMU[brief.shellVariant];
    const footerHeightEmu = DEFAULT_PAGE_HEIGHT_EMU - body.bottom;
    const { raw, contentBottom } = stackModulesRaw(brief.moduleOrder, body.top);
    const naturalPageHeight = contentBottom + footerHeightEmu;
    const pageHeightEmu = Math.max(MIN_PAGE_HEIGHT_EMU, Math.min(MAX_PAGE_HEIGHT_EMU, naturalPageHeight));
    const bottomLimitEmu = pageHeightEmu - footerHeightEmu;
    const placements = raw.map((p) => ({
        key: p.key,
        leftPct: (p.leftEmu / PAGE_WIDTH_EMU) * 100,
        widthPct: (p.widthEmu / PAGE_WIDTH_EMU) * 100,
        topPct: (p.topEmu / pageHeightEmu) * 100,
        heightPct: Math.max((p.heightEmu / pageHeightEmu) * 100, MIN_BLOCK_HEIGHT_PCT),
        fits: p.topEmu + p.heightEmu <= bottomLimitEmu,
    }));
    return { placements, pageHeightEmu };
}

/** Groups fitting placements into visual rows: side-by-side halves share the same top. */
export function groupRows(fitting: Placement[]): Placement[][] {
    const rows: Placement[][] = [];
    for (const p of fitting) {
        const lastRow = rows[rows.length - 1];
        if (lastRow && lastRow[0].topPct === p.topPct) {
            lastRow.push(p);
        } else {
            rows.push([p]);
        }
    }
    return rows;
}

/** Bottom edge (percent) of a row: the lowest of its placements. */
function rowBottom(row: Placement[]): number {
    return Math.max(...row.map((r) => r.topPct + r.heightPct));
}

/**
 * The drop gaps rendered while dragging: one at every row boundary, plus one above the first row
 * and one below the last (or a single centered gap on an empty canvas). `gaps[i]` sits between
 * `rows[i - 1]` and `rows[i]`.
 */
export function computeGaps(rows: Placement[][]): DropGap[] {
    if (rows.length === 0) return [{ id: `${GAP_ID_PREFIX}0`, topPct: EMPTY_CANVAS_GAP_PCT }];
    const gaps = rows.map((row, i) => ({
        id: `${GAP_ID_PREFIX}${i}`,
        topPct: i === 0 ? Math.max(row[0].topPct - GAP_INSET_PCT, 0) : (rowBottom(rows[i - 1]) + row[0].topPct) / 2,
    }));
    gaps.push({
        id: `${GAP_ID_PREFIX}${rows.length}`,
        topPct: Math.min(rowBottom(rows[rows.length - 1]) + GAP_INSET_PCT, 100),
    });
    return gaps;
}

/**
 * Where the dragged module lands in the stacking order (with itself removed) for a drop target:
 * a gap inserts right after the last module of the row above it; the canvas appends after the
 * last fitting module; the overflow tray appends at the very end; another module inserts before
 * that module. Returns null when the target is unknown.
 */
export function dropInsertIndex(
    overId: string,
    order: ModuleKey[],
    activeKey: ModuleKey,
    rows: Placement[][],
    fitting: Placement[],
): number | null {
    const withoutActive = order.filter((k) => k !== activeKey);
    if (overId.startsWith(GAP_ID_PREFIX)) {
        const gapIndex = Number(overId.slice(GAP_ID_PREFIX.length));
        const rowAbove = rows[gapIndex - 1];
        return rowAbove ? withoutActive.indexOf(rowAbove[rowAbove.length - 1].key) + 1 : 0;
    }
    if (overId === CANVAS_ZONE_ID) {
        return fitting.length > 0 ? withoutActive.indexOf(fitting[fitting.length - 1].key) + 1 : 0;
    }
    if (overId === OVERFLOW_ZONE_ID) return withoutActive.length;
    const overIndex = withoutActive.indexOf(overId as ModuleKey);
    return overIndex >= 0 ? overIndex : null;
}

/**
 * The recommended empty slot beside the solution cards, in the grid's right half, when the cards
 * are placed but the companion visual is not; null otherwise.
 */
export function recommendedVisualSlot(fitting: Placement[], order: ModuleKey[]): SlotBox | null {
    const solutions = fitting.find((p) => p.key === "SOLUTIONS");
    if (!solutions || order.includes("SOLUTIONS_VISUAL")) return null;
    return {
        leftPct: solutions.leftPct + solutions.widthPct + (GRID_GAP_EMU / PAGE_WIDTH_EMU) * 100,
        widthPct: solutions.widthPct,
        topPct: solutions.topPct,
        heightPct: solutions.heightPct,
    };
}
