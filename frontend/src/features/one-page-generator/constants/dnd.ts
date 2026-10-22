/** Droppable id of the canvas itself (dropping on empty shell space). */
export const CANVAS_ZONE_ID = "opg-canvas-zone";

/** Droppable id of the overflow tray below the canvas. */
export const OVERFLOW_ZONE_ID = "opg-overflow-zone";

/** Droppable id of the recommended slot beside the solution cards. */
export const SOLUTIONS_VISUAL_SLOT_ID = "opg-solutions-visual-slot";

/** Prefix of the row-boundary drop gaps; the suffix is the gap index. */
export const GAP_ID_PREFIX = "opg-gap-";

/** Pointer travel before a drag starts, so a click on a block still reads as a click. */
export const DRAG_ACTIVATION_DISTANCE = 4;

/** How long the "doesn't fit here" toast stays visible. */
export const REJECT_TOAST_MS = 2600;
