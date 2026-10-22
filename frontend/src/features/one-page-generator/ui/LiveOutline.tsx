import { useState } from "react";

import {
    DndContext,
    PointerSensor,
    closestCenter,
    pointerWithin,
    useDroppable,
    useSensor,
    useSensors,
    type CollisionDetection,
    type DragEndEvent,
    type DragOverEvent,
} from "@dnd-kit/core";
import { SortableContext, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";

import { CANVAS_ZONE_ID, DRAG_ACTIVATION_DISTANCE, OVERFLOW_ZONE_ID, REJECT_TOAST_MS, SOLUTIONS_VISUAL_SLOT_ID } from "../constants/dnd";
import { PAGE_WIDTH_EMU } from "../constants/layout";
import { MODULE_LABELS, SHELLS } from "../constants/modules";
import { computeGaps, computeLayout, dropInsertIndex, groupRows, recommendedVisualSlot } from "../model/layout";
import { moveModule } from "../model/moduleOrder";
import type {
    CanvasBlockProps,
    DropGapProps,
    DropZoneProps,
    LiveOutlineProps,
    ModuleKey,
    OverflowRowProps,
    RecommendedSlotProps,
} from "../model/types";
import { ModulePreview } from "./ModulePreview";

/**
 * Prefers whichever droppable the pointer is literally over (dnd-kit ranks hits smallest area
 * first, so a thin drop gap wins over the large block rects it overlaps), falling back to the
 * nearest center only when the pointer is over nothing at all.
 */
function canvasCollisionDetection(args: Parameters<CollisionDetection>[0]) {
    const pointerHits = pointerWithin(args);
    return pointerHits.length > 0 ? pointerHits : closestCenter(args);
}

/** One module positioned on the shell canvas at its computed location, draggable in place. */
function CanvasBlock({ placement, onRemove }: CanvasBlockProps) {
    const { attributes, listeners, setNodeRef, transform, isDragging } = useSortable({ id: placement.key });
    return (
        <div
            ref={setNodeRef}
            className={`opg__block${isDragging ? " opg__block--dragging" : ""}`}
            style={{
                left: `${placement.leftPct}%`,
                width: `${placement.widthPct}%`,
                top: `${placement.topPct}%`,
                height: `${placement.heightPct}%`,
                transform: CSS.Translate.toString(transform),
            }}
            {...attributes}
            {...listeners}
        >
            <ModulePreview moduleKey={placement.key} />
            <button
                type="button"
                className="opg__block-remove"
                title={`Remove ${MODULE_LABELS[placement.key]}`}
                aria-label={`Remove ${MODULE_LABELS[placement.key]}`}
                onPointerDown={(e) => e.stopPropagation()}
                onClick={(e) => {
                    e.stopPropagation();
                    onRemove(placement.key);
                }}
            >
                ×
            </button>
        </div>
    );
}

/**
 * The empty slot beside the solution cards: the only spot in the master that supports a second
 * module alongside another. Clicking adds the companion visual; dropping anything else is
 * rejected, since nothing else renders there in the assembled page.
 */
function RecommendedSlot({ box, rejecting, onAdd }: RecommendedSlotProps) {
    const { setNodeRef, isOver } = useDroppable({ id: SOLUTIONS_VISUAL_SLOT_ID });
    return (
        <div
            ref={setNodeRef}
            className={`opg__slot${isOver ? " opg__slot--over" : ""}${rejecting ? " opg__slot--reject" : ""}`}
            style={{ left: `${box.leftPct}%`, width: `${box.widthPct}%`, top: `${box.topPct}%`, height: `${box.heightPct}%` }}
        >
            <span className="opg__slot-hint">Solutions visual available</span>
            <button type="button" className="opg__slot-add" onClick={onAdd}>
                + Add
            </button>
        </div>
    );
}

/**
 * A thin insertion target at a row boundary, rendered only while dragging. Plain sortable
 * collision over absolutely positioned, differently sized blocks targeted the wrong row; a gap
 * the pointer is literally inside is unambiguous.
 */
function DropGap({ gap }: DropGapProps) {
    const { setNodeRef, isOver } = useDroppable({ id: gap.id });
    return <div ref={setNodeRef} className={`opg__gap${isOver ? " opg__gap--over" : ""}`} style={{ top: `${gap.topPct}%` }} />;
}

/** Droppable wrapper around a sortable list, so dropping on empty space still lands. */
function DropZone({ id, className, items, children }: DropZoneProps) {
    const { setNodeRef } = useDroppable({ id });
    return (
        <div ref={setNodeRef} className={className}>
            <SortableContext items={items} strategy={verticalListSortingStrategy}>
                {children}
            </SortableContext>
        </div>
    );
}

/** One draggable row of the overflow tray: a module that runs past the page's height ceiling. */
function OverflowRow({ moduleKey, onRemove }: OverflowRowProps) {
    const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: moduleKey });
    const label = MODULE_LABELS[moduleKey];
    return (
        <div
            ref={setNodeRef}
            className={`opg__tray-row${isDragging ? " opg__tray-row--dragging" : ""}`}
            style={{ transform: CSS.Transform.toString(transform), transition }}
            {...attributes}
            {...listeners}
        >
            <span className="opg__tray-handle" aria-hidden="true">⠿</span>
            <span className="opg__tray-swatch" aria-hidden="true">{label.charAt(0)}</span>
            <span className="opg__tray-label">{label}</span>
            <button
                type="button"
                className="opg__tray-remove"
                title={`Remove ${label}`}
                aria-label={`Remove ${label}`}
                onPointerDown={(e) => e.stopPropagation()}
                onClick={(e) => {
                    e.stopPropagation();
                    onRemove(moduleKey);
                }}
            >
                ×
            </button>
        </div>
    );
}

/**
 * The live outline: a canvas of the selected shell (header and footer fixed, as they will
 * render) with every checked module placed as a draggable block at its computed position, and an
 * overflow tray for anything past the page's height ceiling. Both share one stacking order, so a
 * drag between them just moves that module in the sequence the page assembles in.
 */
export function LiveOutline({ brief, onReorder, onRemove, onAddSolutionsVisual }: LiveOutlineProps) {
    const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: DRAG_ACTIVATION_DISTANCE } }));
    const [rejectMessage, setRejectMessage] = useState<string | null>(null);
    const [overInvalidSlot, setOverInvalidSlot] = useState(false);
    const [dragging, setDragging] = useState(false);

    const shell = SHELLS.find((s) => s.value === brief.shellVariant) ?? SHELLS[SHELLS.length - 1];
    const { placements, pageHeightEmu } = computeLayout(brief);
    const fitting = placements.filter((p) => p.fits);
    const overflowing = placements.filter((p) => !p.fits);
    const rows = groupRows(fitting);
    const gaps = computeGaps(rows);
    const slot = recommendedVisualSlot(fitting, brief.moduleOrder);

    function endDrag() {
        setDragging(false);
        setOverInvalidSlot(false);
    }

    function handleDragOver(event: DragOverEvent) {
        setOverInvalidSlot(event.over?.id === SOLUTIONS_VISUAL_SLOT_ID);
    }

    function handleDragEnd(event: DragEndEvent) {
        endDrag();
        const { active, over } = event;
        if (!over) return;
        const activeKey = active.id as ModuleKey;
        const overId = String(over.id);
        if (overId === SOLUTIONS_VISUAL_SLOT_ID) {
            setRejectMessage(`${MODULE_LABELS[activeKey]} doesn't fit here. This slot only fits the solutions visual.`);
            window.setTimeout(() => setRejectMessage(null), REJECT_TOAST_MS);
            return;
        }
        const insertIndex = dropInsertIndex(overId, brief.moduleOrder, activeKey, rows, fitting);
        if (insertIndex === null) return;
        const next = moveModule(brief.moduleOrder, activeKey, insertIndex);
        if (next !== brief.moduleOrder) onReorder(next);
    }

    return (
        <section className="opg__outline" aria-label="Live outline">
            <h2 className="opg__outline-title">Live outline</h2>
            <p className="opg__outline-body">
                This is the shell you picked. Drag a module to reposition it, or hover a block to remove it; anything
                that won't fit drops to Overflow below.
            </p>
            <DndContext
                sensors={sensors}
                collisionDetection={canvasCollisionDetection}
                onDragStart={() => setDragging(true)}
                onDragOver={handleDragOver}
                onDragEnd={handleDragEnd}
                onDragCancel={endDrag}
            >
                <div
                    className={`opg__canvas${overInvalidSlot ? " opg__canvas--reject" : ""}`}
                    style={{ aspectRatio: `${PAGE_WIDTH_EMU} / ${pageHeightEmu}` }}
                >
                    <img src={shell.headerThumbnail} alt="" className="opg__canvas-bg opg__canvas-bg--header" />
                    <img src={shell.footerThumbnail} alt="" className="opg__canvas-bg opg__canvas-bg--footer" />
                    <DropZone id={CANVAS_ZONE_ID} className="opg__canvas-zone" items={fitting.map((p) => p.key)}>
                        {fitting.map((p) => (
                            <CanvasBlock key={p.key} placement={p} onRemove={onRemove} />
                        ))}
                        {dragging && gaps.map((gap) => <DropGap key={gap.id} gap={gap} />)}
                    </DropZone>
                    {slot && <RecommendedSlot box={slot} rejecting={overInvalidSlot} onAdd={onAddSolutionsVisual} />}
                    {brief.moduleOrder.length === 0 && <p className="opg__canvas-empty">Check a module to add it here.</p>}
                    {rejectMessage && (
                        <div className="opg__toast" role="status">
                            {rejectMessage}
                        </div>
                    )}
                </div>

                <div className="opg__tray">
                    <div className="opg__tray-title">Overflow{overflowing.length > 0 ? ` (${overflowing.length})` : ""}</div>
                    <DropZone id={OVERFLOW_ZONE_ID} className="opg__tray-zone" items={overflowing.map((p) => p.key)}>
                        {overflowing.length === 0 ? (
                            <p className="opg__tray-empty">Everything fits the page.</p>
                        ) : (
                            overflowing.map((p) => <OverflowRow key={p.key} moduleKey={p.key} onRemove={onRemove} />)
                        )}
                    </DropZone>
                </div>
            </DndContext>
        </section>
    );
}
