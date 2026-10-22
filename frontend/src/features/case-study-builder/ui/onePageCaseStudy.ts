import { ACCEPTED_SOURCE_FILE_EXTENSIONS, MAX_SOURCE_FILES } from "../constants/form";
import type {
    CreateOnePageRequest,
    MergeFilesResult,
    OnePageField,
    OnePageFieldSection,
    OnePageIcon,
} from "../model/types";

/** The accepted upload extensions as a list, lower-cased, for client-side filtering. */
const ACCEPTED_EXTENSIONS = ACCEPTED_SOURCE_FILE_EXTENSIONS.split(",");

/**
 * Groups drafted fields by `section`, keeping sections in the order they first appear and
 * fields in their original (slide) order within each section.
 */
export function groupFieldsBySection(fields: OnePageField[]): OnePageFieldSection[] {
    const groups: OnePageFieldSection[] = [];
    const byName = new Map<string, OnePageFieldSection>();
    for (const field of fields) {
        let group = byName.get(field.section);
        if (!group) {
            group = { section: field.section, fields: [] };
            byName.set(field.section, group);
            groups.push(group);
        }
        group.fields.push(field);
    }
    return groups;
}

/** True when the file's extension is one the case study drafts accept (case-insensitive). */
export function isAcceptedFile(fileName: string): boolean {
    const dot = fileName.lastIndexOf(".");
    if (dot < 0) return false;
    return ACCEPTED_EXTENSIONS.includes(fileName.slice(dot).toLowerCase());
}

/**
 * Adds newly chosen files to the current list: skips unsupported types and duplicates (same
 * name + size), and caps the list at `max`.
 */
export function mergeSourceFiles<F extends { name: string; size: number }>(
    current: F[],
    chosen: F[],
    max: number = MAX_SOURCE_FILES,
): MergeFilesResult<F> {
    const files = [...current];
    const rejected: string[] = [];
    let overflow = 0;
    for (const file of chosen) {
        if (!isAcceptedFile(file.name)) {
            rejected.push(file.name);
            continue;
        }
        if (files.some((f) => f.name === file.name && f.size === file.size)) continue;
        if (files.length >= max) {
            overflow += 1;
            continue;
        }
        files.push(file);
    }
    return { files, overflow, rejected };
}

/** True when a field is measured in lines on the slide rather than limited by characters. */
export function isMeasured(field: OnePageField): boolean {
    return field.maxLines > 0;
}

/** True when a field's value overflows its box (measured) or its character budget (unmeasured). */
export function isOverBudget(field: OnePageField): boolean {
    return isMeasured(field) ? field.fit === "OVER" : field.value.length > field.maxChars;
}

/** Number of fields whose value overflows its box or character budget. */
export function countOverBudget(fields: OnePageField[]): number {
    return fields.filter(isOverBudget).length;
}

/** Builds the create-deck request from the reviewed fields and icon choices. */
export function buildCreateRequest(
    fields: OnePageField[],
    icons: OnePageIcon[],
    sourceFileNames: string,
): CreateOnePageRequest {
    return {
        fields: fields.map((f) => ({ key: f.key, value: f.value.trim() })),
        icons: icons.map((i) => ({ slot: i.slot, icon: i.icon ?? null })),
        sourceFileNames: sourceFileNames.trim() ? sourceFileNames : null,
    };
}

/** Line target text for a measured field, e.g. "5–6" or "1". */
export function lineTarget(field: OnePageField): string {
    return field.minLines > 0 && field.minLines !== field.maxLines
        ? `${field.minLines}–${field.maxLines}`
        : String(field.maxLines);
}

/** Tooltip describing how much text a field's box holds. */
export function lineTargetTitle(field: OnePageField): string {
    if (!isMeasured(field)) return `Fits up to ${field.maxChars} characters`;
    return field.minLines > 0
        ? `Fills its box at ${lineTarget(field)} lines on the slide`
        : `Fits up to ${field.maxLines} line${field.maxLines === 1 ? "" : "s"} on the slide`;
}

/**
 * Applies a server measurement to the current fields, skipping any field the user edited again
 * while the measurement was in flight so a stale line count never replaces a newer value.
 */
export function applyMeasurement(
    current: OnePageField[],
    sent: OnePageField[],
    measured: OnePageField[],
): OnePageField[] {
    const byKey = new Map(measured.map((m) => [m.key, m]));
    const sentByKey = new Map(sent.map((s) => [s.key, s.value]));
    return current.map((f) => {
        const m = byKey.get(f.key);
        if (!m || sentByKey.get(f.key) !== f.value) return f;
        return { ...f, lines: m.lines, fit: m.fit, note: m.fit === "SHORT" ? (m.note ?? f.note) : null };
    });
}
