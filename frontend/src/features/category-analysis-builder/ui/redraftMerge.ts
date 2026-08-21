import type { StandardDraftField } from "../model/types";

/** A single-slide redraft response: replacement field values for that slide. */
export interface SlideRedraft {
    fields: StandardDraftField[];
}

/** Result of merging a slide redraft back into the full deck state. */
export interface MergedDeck {
    fields: StandardDraftField[];
}

/**
 * Merges a single-slide redraft into the current deck. Only fields whose keys appear in the
 * redraft response are replaced; every other slide's manually edited values are preserved.
 */
export function mergeSlideRedraft(
    prevFields: StandardDraftField[],
    redraft: SlideRedraft,
): MergedDeck {
    const replaced = new Map(redraft.fields.map((f) => [f.key, f.value]));
    const fields = prevFields.map((f) =>
        replaced.has(f.key) ? { ...f, value: replaced.get(f.key) ?? f.value } : f,
    );
    return { fields };
}
