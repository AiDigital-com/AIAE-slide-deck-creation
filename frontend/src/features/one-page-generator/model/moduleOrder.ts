import { ONE_PAGER_BRIEF_ERRORS } from "../constants/form";
import type { BriefErrors, BriefState, CreateRequest, DraftRequest, ModuleKey, OnePagerDraft } from "./types";

/** True when the module is in the stacking order. */
export function isModuleActive(order: ModuleKey[], key: ModuleKey): boolean {
    return order.includes(key);
}

/**
 * Sets whether a module participates in the stacking order: a newly active module is appended,
 * a deactivated one removed, and the relative order of everything else is preserved.
 *
 * Two modules have rules of their own. SOLUTIONS_VISUAL only renders beside SOLUTIONS, so it is
 * inserted right after SOLUTIONS (the live outline pairs adjacent half-width modules into one
 * row), cannot be added without it, and leaves with it.
 */
export function toggleModule(order: ModuleKey[], key: ModuleKey, active: boolean): ModuleKey[] {
    if (key === "SOLUTIONS_VISUAL") {
        return withSolutionsVisual(order, active);
    }
    if (key === "SOLUTIONS" && !active) {
        return order.includes("SOLUTIONS") ? order.filter((k) => k !== "SOLUTIONS" && k !== "SOLUTIONS_VISUAL") : order;
    }
    const has = order.includes(key);
    if (active && !has) return [...order, key];
    if (!active && has) return order.filter((k) => k !== key);
    return order;
}

/** Adds the companion visual right after SOLUTIONS (never without it), or removes it. */
function withSolutionsVisual(order: ModuleKey[], active: boolean): ModuleKey[] {
    const without = order.filter((k) => k !== "SOLUTIONS_VISUAL");
    const solutionsIndex = without.indexOf("SOLUTIONS");
    if (!active || solutionsIndex < 0) return without;
    return [...without.slice(0, solutionsIndex + 1), "SOLUTIONS_VISUAL", ...without.slice(solutionsIndex + 1)];
}

/** Moves a module to the given index of the order with it removed, when that changes anything. */
export function moveModule(order: ModuleKey[], key: ModuleKey, insertIndex: number): ModuleKey[] {
    const withoutActive = order.filter((k) => k !== key);
    const bounded = Math.max(0, Math.min(insertIndex, withoutActive.length));
    const next = [...withoutActive.slice(0, bounded), key, ...withoutActive.slice(bounded)];
    return next.join("|") === order.join("|") ? order : next;
}

/** Validates the brief's text inputs, returning a message per field that is blank. */
export function validateBrief(brief: BriefState): BriefErrors {
    const errors: BriefErrors = {};
    if (!brief.category.trim()) errors.category = ONE_PAGER_BRIEF_ERRORS.category;
    if (!brief.clientName.trim()) errors.clientName = ONE_PAGER_BRIEF_ERRORS.clientName;
    if (!brief.guidanceNotes.trim()) errors.guidanceNotes = ONE_PAGER_BRIEF_ERRORS.guidanceNotes;
    return errors;
}

/** Builds the drafting request from the brief. */
export function buildDraftRequest(brief: BriefState): DraftRequest {
    return {
        category: brief.category.trim(),
        clientName: brief.clientName.trim(),
        guidanceNotes: brief.guidanceNotes.trim(),
        moduleOrder: brief.moduleOrder,
    };
}

/** Builds the page-creation request from the brief and the reviewed draft. */
export function buildCreateRequest(brief: BriefState, draft: OnePagerDraft): CreateRequest {
    return {
        category: brief.category.trim(),
        clientName: brief.clientName.trim(),
        shellVariant: brief.shellVariant,
        content: draft,
    };
}
