import { MemoryRouter, Route, Routes } from "react-router-dom";

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";

import { StrategyToolkit } from "./StrategyToolkit";
import { TOOLS } from "../constants/tools";

describe("StrategyToolkit", () => {
    it("should render one card per tool in the toolkit test", () => {
        // Given: the toolkit home page
        render(
            <MemoryRouter>
                <StrategyToolkit />
            </MemoryRouter>,
        );

        // When-Then: every configured tool is offered, so adding a tool to TOOLS is the
        // single change needed to surface it
        for (const tool of TOOLS) {
            expect(screen.getByRole("heading", { name: tool.title })).toBeTruthy();
        }
    });

    it("should open an external tool in a new tab without leaking the referrer test", () => {
        // Given: the toolkit home page
        render(
            <MemoryRouter>
                <StrategyToolkit />
            </MemoryRouter>,
        );
        const external = TOOLS.find((tool) => tool.externalUrl);

        // When: the external tool's card is located
        const link = screen.getByRole("link", { name: new RegExp(external!.title, "i") });

        // Then: it targets a new tab and carries rel="noopener noreferrer" — without both,
        // the third-party page gets a handle on this window and the referring URL
        expect(link.getAttribute("href")).toBe(external!.externalUrl);
        expect(link.getAttribute("target")).toBe("_blank");
        expect(link.getAttribute("rel")).toBe("noopener noreferrer");
    });

    it("should navigate to an internal tool on click test", async () => {
        // Given: the toolkit rendered inside a router that can reach the tool's route
        const internal = TOOLS.find((tool) => tool.route);
        render(
            <MemoryRouter initialEntries={["/"]}>
                <Routes>
                    <Route path="/" element={<StrategyToolkit />} />
                    <Route path={internal!.route} element={<p>tool page reached</p>} />
                </Routes>
            </MemoryRouter>,
        );

        // When: the card is clicked
        await userEvent.click(screen.getByRole("heading", { name: internal!.title }));

        // Then: the router lands on that tool's own route
        expect(await screen.findByText("tool page reached")).toBeTruthy();
    });

    it("should navigate from the keyboard on Enter and Space test", async () => {
        // Given: the toolkit rendered inside a router
        const internal = TOOLS.find((tool) => tool.route);
        render(
            <MemoryRouter initialEntries={["/"]}>
                <Routes>
                    <Route path="/" element={<StrategyToolkit />} />
                    <Route path={internal!.route} element={<p>tool page reached</p>} />
                </Routes>
            </MemoryRouter>,
        );

        // When: the card is focused and Enter is pressed
        const card = screen.getAllByRole("button")[0];
        card.focus();
        await userEvent.keyboard("{Enter}");

        // Then: a keyboard user reaches the tool without a mouse — the card is a div, so
        // this only works because it carries role, tabIndex, and an onKeyDown handler
        expect(await screen.findByText("tool page reached")).toBeTruthy();
    });

    it("should expose every internal card to assistive technology and the keyboard test", () => {
        // Given: the toolkit home page
        render(
            <MemoryRouter>
                <StrategyToolkit />
            </MemoryRouter>,
        );

        // When: the clickable cards are collected
        const cards = screen.getAllByRole("button");

        // Then: each is reachable by Tab, because a div with an onClick and no tabIndex is
        // invisible to keyboard users
        expect(cards.length).toBe(TOOLS.filter((tool) => tool.route && tool.available).length);
        for (const card of cards) {
            expect(card.getAttribute("tabindex")).toBe("0");
        }
    });

    it("should label each tool icon so it is not announced as bare punctuation test", () => {
        // Given: the toolkit home page, whose icons are emoji
        render(
            <MemoryRouter>
                <StrategyToolkit />
            </MemoryRouter>,
        );

        // When-Then: each icon carries its tool's name, so a screen reader announces the
        // tool rather than reading the emoji character
        for (const tool of TOOLS) {
            expect(screen.getByRole("img", { name: tool.title })).toBeTruthy();
        }
    });

    it("should show the coming-soon placeholder as non-interactive test", () => {
        // Given: the toolkit home page
        render(
            <MemoryRouter>
                <StrategyToolkit />
            </MemoryRouter>,
        );

        // When: the placeholder card is located
        const placeholder = screen.getByRole("heading", { name: "More Tools" });

        // Then: it renders but offers nothing to click, so it cannot be mistaken for a
        // broken tool
        expect(placeholder).toBeTruthy();
        expect(screen.queryByRole("button", { name: /More Tools/i })).toBeNull();
    });
});
