import { Link } from "react-router-dom";

import { UserButton } from "@clerk/clerk-react";

import type { AppHeaderProps } from "./model/types";
import "./app-shell.css";

/** Derives the two-letter brand mark shown left of the product name. */
function brandInitials(name: string): string {
    const letters = name
        .split(/\s+/)
        .filter((w) => /[a-zA-Z0-9]/.test(w))
        .map((w) => w[0]!.toUpperCase());
    return letters.slice(0, 2).join("") || "AI";
}

/** Top app header — Elevate layout (no left sidebar). */
export function AppHeader({ appName }: AppHeaderProps) {
    return (
        <header className="app-header">
            <Link to="/" className="app-header__brand">
                <span className="app-header__logo" aria-hidden="true">
                    {brandInitials(appName)}
                </span>
                <span className="app-header__brand-group">
                    <span className="app-header__brand-name">{appName}</span>
                    <span className="app-header__brand-sub">AI Digital Toolkit</span>
                </span>
            </Link>
            <div className="app-header__actions">
                <UserButton afterSignOutUrl="/login" />
            </div>
        </header>
    );
}
