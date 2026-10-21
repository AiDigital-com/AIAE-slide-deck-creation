/** TanStack Query key of the One Page Generator connection check. */
export const ONE_PAGER_CONNECTIONS_QUERY_KEY = ["one-pagers", "connections"] as const;

/** Message shown when drafting fails without a usable response body. */
export const ONE_PAGER_DRAFT_FALLBACK_ERROR = "Drafting failed. Check the AI connection and try again.";

/** Message shown when page creation fails without a usable response body. */
export const ONE_PAGER_DECK_FALLBACK_ERROR = "Page creation failed. Check Google access and try again.";

/**
 * Contract field names mapped to the labels the builder shows, so a rejected request names the
 * input the user can actually see and fix.
 */
export const ONE_PAGER_FIELD_LABELS: Record<string, string> = {
    category: "Category / vertical",
    clientName: "Client / label",
    guidanceNotes: "Guidance brief",
    moduleOrder: "Modules",
    shellVariant: "Shell",
    content: "Page content",
};

/** Client-side validation messages for the brief form. */
export const ONE_PAGER_BRIEF_ERRORS = {
    category: "Enter the category or vertical the page targets.",
    clientName: "Enter the client or label used in the file's title.",
    guidanceNotes: "Paste the guidance brief — it is the only source of facts the drafter uses.",
} as const;

/** Most title lines the header holds. */
export const MAX_TITLE_LINES = 3;

/** Most checklist bullets a card holds. */
export const MAX_CARD_BULLETS = 3;

/** Rows given to the guidance brief textarea. */
export const GUIDANCE_NOTES_ROWS = 10;
