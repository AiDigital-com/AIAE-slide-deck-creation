import { describe, expect, it } from "vitest";

import { mergeInDepthSlideRedraft, type InDepthSlideRedraft } from "./inDepthRedraftMerge";
import type { InDepthDraftField } from "../model/types";

/**
 * Builds a small checked-deck field set: two slide-specific fields plus one field shared across
 * two checked layout variants of the same topic (mirrors slides 3/4 in the real deck).
 */
function draftCheckedDeck(): InDepthDraftField[] {
    return [
        { key: "trend_headline", label: "Headline", slideNumbers: [2], value: "ai draft trend headline" },
        { key: "trend_intro", label: "Intro", slideNumbers: [2], value: "ai draft trend intro" },
        { key: "highlights_headline", label: "Headline", slideNumbers: [3, 4], value: "ai draft highlights headline" },
        { key: "hi_headline", label: "Headline", slideNumbers: [16], value: "ai draft hi headline" },
    ];
}

/** Applies a manual edit to one field, mimicking the user typing in the review form. */
function editField(fields: InDepthDraftField[], key: string, value: string): InDepthDraftField[] {
    return fields.map((f) => (f.key === key ? { ...f, value } : f));
}

describe("mergeInDepthSlideRedraft", () => {
    it("should replace only the redrafted keys and preserve edits on every other field test", () => {
        // Given: a drafted deck with manual edits on two fields outside the redrafted slide
        let fields = draftCheckedDeck();
        fields = editField(fields, "trend_intro", "MY EDIT trend intro");
        fields = editField(fields, "hi_headline", "MY EDIT hi headline");
        const redraft: InDepthSlideRedraft = {
            fields: [
                { key: "trend_headline", label: "Headline", slideNumbers: [2], value: "redrafted trend headline" },
            ],
        };

        // When: the redraft is merged
        const merged = mergeInDepthSlideRedraft(fields, redraft);
        const byKey = new Map(merged.fields.map((f) => [f.key, f.value]));

        // Then: the redrafted key changed and nothing else did
        expect(byKey.get("trend_headline")).toBe("redrafted trend headline");
        expect(byKey.get("trend_intro")).toBe("MY EDIT trend intro");
        expect(byKey.get("hi_headline")).toBe("MY EDIT hi headline");
        expect(byKey.get("highlights_headline")).toBe("ai draft highlights headline");
    });

    it("should update a shared field once for every slide that shows it test", () => {
        // Given: slides 3 and 4 share the highlights headline
        const fields = draftCheckedDeck();
        const redraft: InDepthSlideRedraft = {
            fields: [
                { key: "highlights_headline", label: "Headline", slideNumbers: [3, 4], value: "redrafted shared headline" },
            ],
        };

        // When: the redraft arrives via one of the variants
        const merged = mergeInDepthSlideRedraft(fields, redraft);

        // Then: the single shared entry carries the new value and is not duplicated per slide
        const shared = merged.fields.find((f) => f.key === "highlights_headline");
        expect(shared?.value).toBe("redrafted shared headline");
        expect(merged.fields.filter((f) => f.key === "highlights_headline")).toHaveLength(1);
    });

    it("should not mutate the input fields array test", () => {
        // Given: a snapshot of the current fields
        const fields = draftCheckedDeck();
        const snapshot = fields.map((f) => ({ ...f }));

        // When: a redraft is merged
        mergeInDepthSlideRedraft(fields, {
            fields: [{ key: "trend_headline", label: "Headline", slideNumbers: [2], value: "x" }],
        });

        // Then: the original array is untouched
        expect(fields).toEqual(snapshot);
    });
});
