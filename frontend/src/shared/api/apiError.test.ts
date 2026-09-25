import { describe, expect, it } from "vitest";

import { describeApiError } from "./apiError";

describe("describeApiError", () => {
    const labels = { guidanceNotes: "Focus notes", category: "Category" };

    it("names the rejected field and restates its character budget", () => {
        const body = {
            errors: [
                { code: "Size", field: "guidanceNotes", error: "size must be between 0 and 10000" },
            ],
            timestamp: "2026-09-25T09:35:20.801626495",
            correlationId: "030245f4-6cd7-476c-a441-0420fe70af03",
        };

        expect(describeApiError(body, "fallback", labels)).toBe(
            "Focus notes: must be 10,000 characters or fewer.",
        );
    });

    it("reports every rejected field in one message", () => {
        const body = {
            errors: [
                { code: "Size", field: "guidanceNotes", error: "size must be between 0 and 10000" },
                { code: "NotBlank", field: "category", error: "must not be blank" },
            ],
        };

        expect(describeApiError(body, "fallback", labels)).toBe(
            "Focus notes: must be 10,000 characters or fewer. Category: must not be blank.",
        );
    });

    it("falls back to the contract field name when the form has no label for it", () => {
        const body = { errors: [{ code: "Size", field: "unmapped", error: "must not be blank" }] };

        expect(describeApiError(body, "fallback", labels)).toBe("unmapped: must not be blank.");
    });

    it("keeps an application failure's own message and its support reference", () => {
        const body = {
            code: "C003",
            message: "AI drafting call failed with HTTP 429",
            correlationId: "abc-123",
        };

        expect(describeApiError(body, "fallback", labels)).toBe(
            "AI drafting call failed with HTTP 429 (reference abc-123)",
        );
    });

    it("uses the fallback when the response carried no body", () => {
        expect(describeApiError(undefined, "Drafting failed.", labels)).toBe("Drafting failed.");
    });

    it("attaches the reference to the fallback when only a correlation id is present", () => {
        expect(describeApiError({ correlationId: "abc-123" }, "Drafting failed.", labels)).toBe(
            "Drafting failed. (reference abc-123)",
        );
    });
});
