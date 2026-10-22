import { describe, expect, it } from "vitest";

import { ONE_PAGER_BRIEF_ERRORS } from "../constants/form";
import { EMPTY_BRIEF } from "../constants/modules";
import {
    buildCreateRequest,
    buildDraftRequest,
    isModuleActive,
    moveModule,
    toggleModule,
    validateBrief,
} from "./moduleOrder";
import { replaceAt, splitLines } from "./review";
import type { BriefState, ModuleKey, OnePagerDraft } from "./types";

/** A brief with the given modules and otherwise valid text. */
function brief(moduleOrder: ModuleKey[], overrides: Partial<BriefState> = {}): BriefState {
    return { ...EMPTY_BRIEF, category: "Automotive", clientName: "Acme", guidanceNotes: "Brief", moduleOrder, ...overrides };
}

/** A header-only draft. */
function draft(): OnePagerDraft {
    return {
        topicPill: "AUTO",
        mainTitle: ["ONE"],
        supportingCopy: "Copy",
        moduleOrder: ["KPIS"],
        processSteps: [],
        solutions: [],
        kpis: [],
        impactStats: [],
        capabilityCards: [],
        spotlightCards: [],
        insightCards: [],
        milestones: [],
    };
}

describe("toggleModule", () => {
    it("should append a newly active module and drop a deactivated one, keeping the rest in order test", () => {
        // Given: two modules in order
        const order: ModuleKey[] = ["SOLUTIONS", "KPIS"];

        // When: a module is added, re-added, removed, and re-removed
        const added = toggleModule(order, "TRACKER", true);
        const unchanged = toggleModule(added, "TRACKER", true);
        const removed = toggleModule(added, "SOLUTIONS", false);
        const stillRemoved = toggleModule(removed, "SOLUTIONS", false);

        // Then: additions go to the end, removals keep the others' order, and no-ops return the same list
        expect(added).toEqual(["SOLUTIONS", "KPIS", "TRACKER"]);
        expect(unchanged).toBe(added);
        expect(removed).toEqual(["KPIS", "TRACKER"]);
        expect(stillRemoved).toBe(removed);
        expect(isModuleActive(added, "TRACKER")).toBe(true);
        expect(isModuleActive(removed, "SOLUTIONS")).toBe(false);
    });

    it("should keep the solutions visual right after the solution cards and never without them test", () => {
        // Given: solutions between two other modules
        const order: ModuleKey[] = ["TRACKER", "SOLUTIONS", "KPIS"];

        // When: the visual is added, then solutions are removed, then the visual is added without solutions
        const withVisual = toggleModule(order, "SOLUTIONS_VISUAL", true);
        const withoutSolutions = toggleModule(withVisual, "SOLUTIONS", false);
        const orphan = toggleModule(["KPIS"], "SOLUTIONS_VISUAL", true);
        const removedVisual = toggleModule(withVisual, "SOLUTIONS_VISUAL", false);

        // Then: the visual sits beside solutions, leaves with them, and cannot exist alone
        expect(withVisual).toEqual(["TRACKER", "SOLUTIONS", "SOLUTIONS_VISUAL", "KPIS"]);
        expect(withoutSolutions).toEqual(["TRACKER", "KPIS"]);
        expect(orphan).toEqual(["KPIS"]);
        expect(removedVisual).toEqual(["TRACKER", "SOLUTIONS", "KPIS"]);
    });
});

describe("moveModule", () => {
    it("should move a module to the requested slot and return the same list when nothing changes test", () => {
        // Given: three modules
        const order: ModuleKey[] = ["TRACKER", "SOLUTIONS", "KPIS"];

        // When-Then: moving to the front, the end (clamped), and its own place
        expect(moveModule(order, "KPIS", 0)).toEqual(["KPIS", "TRACKER", "SOLUTIONS"]);
        expect(moveModule(order, "TRACKER", 99)).toEqual(["SOLUTIONS", "KPIS", "TRACKER"]);
        expect(moveModule(order, "SOLUTIONS", 1)).toBe(order);
    });
});

describe("validateBrief", () => {
    it("should name every blank text field and accept a complete brief test", () => {
        // Given: a brief with whitespace-only text and a complete one
        const blank = brief([], { category: " ", clientName: "", guidanceNotes: "\n" });

        // When: both are validated
        const errors = validateBrief(blank);
        const valid = validateBrief(brief(["KPIS"]));

        // Then: each blank field carries its message, the complete brief none
        expect(errors).toEqual({
            category: ONE_PAGER_BRIEF_ERRORS.category,
            clientName: ONE_PAGER_BRIEF_ERRORS.clientName,
            guidanceNotes: ONE_PAGER_BRIEF_ERRORS.guidanceNotes,
        });
        expect(valid).toEqual({});
    });
});

describe("requests", () => {
    it("should trim the brief text and carry the module order, shell, and draft test", () => {
        // Given: a brief with padded text
        const padded = brief(["SOLUTIONS", "KPIS"], { category: " Automotive ", clientName: "Acme ", shellVariant: "BLUE_HEADER" });

        // When: both requests are built
        const draftRequest = buildDraftRequest(padded);
        const createRequest = buildCreateRequest(padded, draft());

        // Then: text is trimmed and the structural fields pass through
        expect(draftRequest).toEqual({
            category: "Automotive",
            clientName: "Acme",
            guidanceNotes: "Brief",
            moduleOrder: ["SOLUTIONS", "KPIS"],
        });
        expect(createRequest.shellVariant).toBe("BLUE_HEADER");
        expect(createRequest.category).toBe("Automotive");
        expect(createRequest.content).toEqual(draft());
    });
});

describe("review helpers", () => {
    it("should replace one item and split lines up to the cap test", () => {
        // Given-When-Then: replacement is positional and splitting drops blank lines past the cap
        expect(replaceAt(["a", "b", "c"], 1, "x")).toEqual(["a", "x", "c"]);
        expect(splitLines("one\n\ntwo\nthree\nfour", 3)).toEqual(["one", "two", "three"]);
        expect(splitLines("", 3)).toEqual([]);
    });
});
