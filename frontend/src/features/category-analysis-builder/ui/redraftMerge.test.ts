import { describe, expect, it } from "vitest";

import { mergeSlideRedraft, type SlideRedraft } from "./redraftMerge";
import type { components } from "../../../shared/api/generated/schema";

type StandardDraftField = components["schemas"]["StandardDraftFieldV1"];

/** Builds a full 5-slide deck: two editable fields per slide, keyed s{slide}_{n}. */
function draftFullDeck(): StandardDraftField[] {
    const fields: StandardDraftField[] = [];
    for (let slide = 1; slide <= 5; slide += 1) {
        for (let n = 1; n <= 2; n += 1) {
            fields.push({
                key: `s${slide}_${n}`,
                label: `Slide ${slide} field ${n}`,
                slideNumber: slide,
                value: `ai draft s${slide}_${n}`,
            });
        }
    }
    return fields;
}

/** Applies a manual edit to one field, mimicking the user typing in the review form. */
function editField(fields: StandardDraftField[], key: string, value: string): StandardDraftField[] {
    return fields.map((f) => (f.key === key ? { ...f, value } : f));
}

/** A redraft response for a slide: new values for that slide's fields. */
function redraftResponse(slide: number): SlideRedraft {
    return {
        fields: [
            { key: `s${slide}_1`, label: `Slide ${slide} field 1`, slideNumber: slide, value: `redrafted s${slide}_1` },
            { key: `s${slide}_2`, label: `Slide ${slide} field 2`, slideNumber: slide, value: `redrafted s${slide}_2` },
        ],
    };
}

describe("mergeSlideRedraft", () => {
    it("replaces only the redrafted slide's fields and preserves edits on every other slide", () => {
        let fields = draftFullDeck();
        // The user manually edits fields across several different slides.
        fields = editField(fields, "s1_1", "MY EDIT slide 1");
        fields = editField(fields, "s3_2", "MY EDIT slide 3");
        fields = editField(fields, "s4_1", "MY EDIT slide 4");
        fields = editField(fields, "s5_2", "MY EDIT slide 5");

        // Redraft slide 3 only.
        const merged = mergeSlideRedraft(fields, redraftResponse(3));
        const byKey = new Map(merged.fields.map((f) => [f.key, f.value]));

        // Slide 3's fields are replaced with the redraft values.
        expect(byKey.get("s3_1")).toBe("redrafted s3_1");
        expect(byKey.get("s3_2")).toBe("redrafted s3_2");

        // Edits on slides 1, 4 and 5 are untouched.
        expect(byKey.get("s1_1")).toBe("MY EDIT slide 1");
        expect(byKey.get("s4_1")).toBe("MY EDIT slide 4");
        expect(byKey.get("s5_2")).toBe("MY EDIT slide 5");

        // Every non-slide-3 field is byte-for-byte identical to the pre-redraft state.
        for (const before of fields) {
            if (before.slideNumber === 3) continue;
            expect(byKey.get(before.key)).toBe(before.value);
        }
    });

    it("does not mutate the input fields array", () => {
        const fields = draftFullDeck();
        const snapshot = fields.map((f) => ({ ...f }));
        mergeSlideRedraft(fields, redraftResponse(2));
        expect(fields).toEqual(snapshot);
    });
});
