import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { RECENT_PAGES_EMPTY, RECENT_PAGES_LOAD_ERROR } from "../constants/form";
import type { RecentPage } from "../model/types";
import { RecentPages } from "./RecentPages";

/** A recorded one-pager row. */
function page(id: number, overrides: Partial<RecentPage> = {}): RecentPage {
    return {
        id,
        title: `One Page Generator — Automotive — Client ${id}`,
        category: "Automotive",
        slidesUrl: `https://docs.google.com/presentation/d/${id}/edit`,
        templateKind: "ONE_PAGER",
        status: "SUBMITTED",
        createdAt: "2026-10-21T09:00:00Z",
        ...overrides,
    };
}

describe("RecentPages", () => {
    it("should show the loading state while the list is fetched test", () => {
        // Given: the query has not answered yet
        render(<RecentPages pages={[]} loading error={false} />);

        // When-Then: a loading indicator is shown and no empty copy competes with it
        expect(screen.getByRole("status")).toBeTruthy();
        expect(screen.queryByText(RECENT_PAGES_EMPTY)).toBeNull();
    });

    it("should show an error when the list could not be loaded test", () => {
        // Given: the query failed
        render(<RecentPages pages={[]} loading={false} error />);

        // When-Then: the failure is announced as an alert
        expect(screen.getByRole("alert").textContent).toBe(RECENT_PAGES_LOAD_ERROR);
    });

    it("should show the empty copy when the caller has no pages yet test", () => {
        // Given: a successful but empty list
        render(<RecentPages pages={[]} loading={false} error={false} />);

        // When-Then: the empty copy is shown
        expect(screen.getByText(RECENT_PAGES_EMPTY)).toBeTruthy();
    });

    it("should list each page with a link that opens its Slides file in a new tab test", () => {
        // Given: two recorded pages, one of which has no link yet
        render(
            <RecentPages pages={[page(2), page(1, { slidesUrl: null, status: "DRAFT" })]} loading={false} error={false} />,
        );

        // When: the links are located
        const links = screen.getAllByRole("link", { name: /open page/i });

        // Then: only the page with a file gets a link, it targets a new tab, and the other shows its state
        expect(links).toHaveLength(1);
        expect(links[0].getAttribute("href")).toBe("https://docs.google.com/presentation/d/2/edit");
        expect(links[0].getAttribute("target")).toBe("_blank");
        expect(screen.getByText("One Page Generator — Automotive — Client 1")).toBeTruthy();
        expect(screen.getByText("Draft")).toBeTruthy();
    });
});
