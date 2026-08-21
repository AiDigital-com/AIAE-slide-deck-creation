import { SignIn } from "@clerk/clerk-react";

import { APP_NAME, BRAND_EYEBROW, LOGIN_SUBTITLE } from "./constants/login";
import "./login.css";

// Login — Clerk SSO sign-in (the only auth mode). Clerk renders and manages the
// full sign-in flow; on success it redirects per
// VITE_CLERK_SIGN_IN_FORCE_REDIRECT_URL. There is no mock-login form.
//
// Split layout: the brand panel carries the product identity so the page reads
// as this product rather than as a bare Clerk widget, and the sign-in card sits
// in its own column. The brand panel is decorative and collapses on narrow
// viewports, so the form is never pushed below the fold on a phone.

/** Derives the brand mark from the product name, matching the in-app header. */
function brandInitials(name: string): string {
    const letters = name
        .split(/\s+/)
        .filter((word) => /[a-zA-Z0-9]/.test(word))
        .map((word) => word[0]!.toUpperCase());
    return letters.slice(0, 2).join("") || "AI";
}

export default function Login() {
    return (
        <main className="login">
            <div className="login__shell">
                <section className="login__brand">
                    <span className="login__mark" aria-hidden="true">
                        {brandInitials(APP_NAME)}
                    </span>
                    <p className="login__eyebrow">{BRAND_EYEBROW}</p>
                    <h1 className="login__title">{APP_NAME}</h1>
                    <p className="login__lede">{LOGIN_SUBTITLE}</p>
                </section>
                <section className="login__panel">
                    <h2 className="login__panel-title">Log in</h2>
                    <SignIn />
                </section>
            </div>
        </main>
    );
}
