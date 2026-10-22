import { describe, expect, it } from "vitest";

import {
    applyMeasurement,
    buildCreateRequest,
    countOverBudget,
    groupFieldsBySection,
    isAcceptedFile,
    lineTarget,
    lineTargetTitle,
    mergeSourceFiles,
} from "./onePageCaseStudy";
import type { OnePageField, OnePageIcon } from "../model/types";

/** Builds a character-limited field (no box) with the given value and budget. */
function field(key: string, section: string, value = "", maxChars = 40): OnePageField {
    return {
        key,
        label: key,
        section,
        value,
        source: "FILES",
        maxChars,
        minLines: 0,
        maxLines: 0,
        lines: 0,
        fit: value ? "FITS" : "EMPTY",
    };
}

function file(name: string, size = 1) {
    return { name, size };
}

describe("groupFieldsBySection", () => {
    it("should keep sections in first-seen order and fields in slide order test", () => {
        // Given: fields whose sections interleave
        const fields = [
            field("title", "Header"),
            field("challenge", "Challenge"),
            field("subtitle", "Header"),
            field("tactic_1_title", "Tactics"),
            field("challenge_body", "Challenge"),
        ];

        // When: they are grouped
        const groups = groupFieldsBySection(fields);

        // Then: each section appears once, in first-seen order, with its fields in slide order
        expect(groups.map((g) => g.section)).toEqual(["Header", "Challenge", "Tactics"]);
        expect(groups[0].fields.map((f) => f.key)).toEqual(["title", "subtitle"]);
        expect(groups[1].fields.map((f) => f.key)).toEqual(["challenge", "challenge_body"]);
        expect(groupFieldsBySection([])).toEqual([]);
    });
});

describe("isAcceptedFile", () => {
    it("should accept supported extensions case-insensitively and reject the rest test", () => {
        // Given-When-Then: every supported kind passes regardless of case; others and bare names fail
        expect(isAcceptedFile("report.PDF")).toBe(true);
        expect(isAcceptedFile("notes.md")).toBe(true);
        expect(isAcceptedFile("screenshot.webp")).toBe(true);
        expect(isAcceptedFile("deck.final.pptx")).toBe(true);
        expect(isAcceptedFile("video.mp4")).toBe(false);
        expect(isAcceptedFile("README")).toBe(false);
    });
});

describe("mergeSourceFiles", () => {
    it("should skip duplicates and unsupported types test", () => {
        // Given: one file already chosen
        const current = [file("a.pdf")];

        // When: a duplicate, an unsupported file, and a new screenshot are added
        const result = mergeSourceFiles(current, [file("a.pdf"), file("b.zip"), file("c.png")]);

        // Then: only the screenshot joins and the zip is reported
        expect(result.files.map((f) => f.name)).toEqual(["a.pdf", "c.png"]);
        expect(result.rejected).toEqual(["b.zip"]);
        expect(result.overflow).toBe(0);
    });

    it("should cap the list at the maximum and report the overflow test", () => {
        // Given: one file chosen and a cap of three
        const current = [file("a.pdf")];

        // When: three more are added
        const result = mergeSourceFiles(current, [file("b.pdf"), file("c.pdf"), file("d.pdf")], 3);

        // Then: the list stops at three and one overflow is reported
        expect(result.files.map((f) => f.name)).toEqual(["a.pdf", "b.pdf", "c.pdf"]);
        expect(result.overflow).toBe(1);
    });
});

describe("countOverBudget", () => {
    it("should count character overflow for unmeasured fields and fit for measured ones test", () => {
        // Given: one over-budget character field, one within, one blank, one measured OVER, one measured FITS
        const over = { ...field("a", "s", "x"), maxLines: 1, fit: "OVER" as const };
        const fits = { ...field("b", "s", "y".repeat(500)), maxLines: 2, fit: "FITS" as const };

        // When-Then: character fields count by length, measured fields by fit only
        expect(countOverBudget([field("a", "S", "12345", 5), field("b", "S", "123456", 5), field("c", "S", "", 5)])).toBe(1);
        expect(countOverBudget([over, fits])).toBe(1);
    });
});

describe("buildCreateRequest", () => {
    it("should send trimmed values and icon choices, keeping null for template icons test", () => {
        // Given: an edited title with padding, a blank stat, and icons of every kind
        const fields = [field("title", "Header", "  Edited title  "), field("stat_1", "Stats", "")];
        const icons: OnePageIcon[] = [
            { slot: "tactic_1_icon", label: "Tactic 1", icon: "brain" },
            { slot: "tactic_2_icon", label: "Tactic 2", icon: null },
            { slot: "tactic_3_icon", label: "Tactic 3" },
        ];

        // When: the request is built
        const request = buildCreateRequest(fields, icons, "a.pdf, b.png");

        // Then: values are trimmed and a missing icon becomes an explicit null
        expect(request).toEqual({
            fields: [
                { key: "title", value: "Edited title" },
                { key: "stat_1", value: "" },
            ],
            icons: [
                { slot: "tactic_1_icon", icon: "brain" },
                { slot: "tactic_2_icon", icon: null },
                { slot: "tactic_3_icon", icon: null },
            ],
            sourceFileNames: "a.pdf, b.png",
        });
        expect(buildCreateRequest([], [], "  ").sourceFileNames).toBeNull();
    });
});

describe("line targets", () => {
    it("should show a range when the box has a fill target and a limit otherwise test", () => {
        // Given: a 5–6 line challenge, a one-line title, and a character-limited stat
        const challenge = { ...field("challenge", "s", "text"), minLines: 5, maxLines: 6, lines: 3 };
        const title = { ...field("title", "s", "text"), minLines: 0, maxLines: 1, lines: 1 };
        const stat = field("stat", "s", "-68%", 7);

        // When-Then: the target text and tooltip describe each box
        expect(lineTarget(challenge)).toBe("5–6");
        expect(lineTarget(title)).toBe("1");
        expect(lineTargetTitle(challenge)).toBe("Fills its box at 5–6 lines on the slide");
        expect(lineTargetTitle(title)).toBe("Fits up to 1 line on the slide");
        expect(lineTargetTitle(stat)).toBe("Fits up to 7 characters");
    });
});

describe("applyMeasurement", () => {
    it("should apply line counts only to fields unchanged since they were sent test", () => {
        // Given: two fields sent for measuring, one of which the user edited meanwhile
        const sent = [
            { ...field("challenge", "s", "old text"), maxLines: 6 },
            { ...field("goal", "s", "the goal"), maxLines: 3 },
        ];
        const current = [{ ...sent[0], value: "new text" }, sent[1]];
        const measured: OnePageField[] = [
            { ...sent[0], lines: 2, fit: "SHORT", note: "server note" },
            { ...sent[1], lines: 1, fit: "FITS", note: null },
        ];

        // When: the measurement arrives
        const result = applyMeasurement(current, sent, measured);

        // Then: the edited field keeps its newer value untouched; the other takes the server's verdict
        expect(result[0].value).toBe("new text");
        expect(result[0].lines).toBe(0);
        expect(result[1].lines).toBe(1);
        expect(result[1].fit).toBe("FITS");
        expect(result[1].note).toBeNull();
    });

    it("should keep the reviewer note only while a field is still short test", () => {
        // Given: a short field with a server note
        const sent = [{ ...field("challenge", "s", "short"), maxLines: 6 }];
        const measured: OnePageField[] = [{ ...sent[0], lines: 2, fit: "SHORT", note: "add detail" }];

        // When: the measurement is applied
        const result = applyMeasurement(sent, sent, measured);

        // Then: the note is shown with the SHORT verdict
        expect(result[0].note).toBe("add detail");
        expect(result[0].fit).toBe("SHORT");
    });
});
