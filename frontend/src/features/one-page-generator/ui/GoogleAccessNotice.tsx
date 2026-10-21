import { useClerk } from "@clerk/clerk-react";

import type { GoogleAccessNoticeProps } from "../model/types";

/**
 * Targeted fix-it notice for the user's Google connection; nothing when access is granted. The
 * same Clerk Google connection backs every deck tool, so the fix mirrors the other builders.
 */
export function GoogleAccessNotice({ status }: GoogleAccessNoticeProps) {
    const clerk = useClerk();
    if (status === "CONNECTED") return null;
    const notConnected = status === "NOT_CONNECTED";
    return (
        <div className="opg__notice opg__notice--warning">
            <p className="opg__notice-title">
                {notConnected ? "You're not signed in with Google" : "Google Drive & Slides access wasn't granted"}
            </p>
            <p className="opg__notice-body">
                {notConnected
                    ? "The page is saved to your own Google Drive. Sign in with your Google account to continue."
                    : "You're signed in with Google, but Drive & Slides access wasn't allowed. Reconnect Google and allow Drive & Slides access."}
            </p>
            {notConnected ? (
                <button type="button" className="opg__notice-action" onClick={() => clerk.signOut({ redirectUrl: "/login" })}>
                    Sign in with Google
                </button>
            ) : (
                <button type="button" className="opg__notice-action" onClick={() => clerk.openUserProfile()}>
                    Reconnect Google
                </button>
            )}
        </div>
    );
}
