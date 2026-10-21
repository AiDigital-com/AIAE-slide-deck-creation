import type { InDepthDraftField } from "../model/types";

/** A single-slide redraft response: replacement field values for that slide's group. */
export interface InDepthSlideRedraft {
    fields: InDepthDraftField[];
}

/** Result of merging a slide redraft back into the checked deck's full field set. */
export interface MergedInDepthDeck {
    fields: InDepthDraftField[];
}

/**
 * Merges a single-slide redraft into the current checked-deck fields. Only fields whose keys
 * appear in the redraft response are replaced; every other field's manually edited value is
 * preserved — including fields shared with slides outside the redrafted one.
 */
export function mergeInDepthSlideRedraft(
    prevFields: InDepthDraftField[],
    redraft: InDepthSlideRedraft,
): MergedInDepthDeck {
    const replaced = new Map(redraft.fields.map((f) => [f.key, f.value]));
    const fields = prevFields.map((f) =>
        replaced.has(f.key) ? { ...f, value: replaced.get(f.key) ?? f.value } : f,
    );
    return { fields };
}
