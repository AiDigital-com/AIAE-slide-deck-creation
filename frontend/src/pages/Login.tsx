import { SignIn } from "@clerk/clerk-react";

import { LOGIN_TITLE } from "./constants/login";
import "./login.css";

// Login — Clerk SSO sign-in (the only auth mode). Clerk renders and manages the
// full sign-in flow; on success it redirects per
// VITE_CLERK_SIGN_IN_FORCE_REDIRECT_URL. There is no mock-login form.
//
// The page is a full-viewport brand surface: the Clerk card sits centered on
// the solid brand blue under a display-caps title, matching the in-app header
// band, so the form is never pushed below the fold on a phone.

export default function Login() {
    return (
        <main className="login">
            <h1 className="login__title">{LOGIN_TITLE}</h1>
            <SignIn />
        </main>
    );
}
