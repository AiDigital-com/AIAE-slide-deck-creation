import { Link } from "react-router-dom";

import { UserButton } from "@clerk/clerk-react";

import type { AppHeaderProps } from "./model/types";
import "./app-shell.css";

/** Top app header — solid brand band, AI Digital wordmark + app name (no left sidebar, no nav links). */
export function AppHeader({ appName }: AppHeaderProps) {
    return (
        <header className="app-header">
            <Link to="/" className="app-header__brand" aria-label={`AI Digital ${appName} home`}>
                <span className="app-header__wordmark" aria-hidden="true">
                    AI<span className="app-header__wordmark-accent">DIGITAL</span>
                </span>
                <span className="app-header__divider" aria-hidden="true" />
                <span className="app-header__app-name">{appName}</span>
            </Link>
            <div className="app-header__actions">
                <UserButton afterSignOutUrl="/login" />
            </div>
        </header>
    );
}
