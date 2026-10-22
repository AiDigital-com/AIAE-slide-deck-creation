/** Fields whose box holds more than one line, or whose character budget exceeds this, get a textarea. */
export const ONE_PAGE_TEXTAREA_THRESHOLD = 60;

/** Fewest and most rows a one-page textarea is given. */
export const ONE_PAGE_TEXTAREA_MIN_ROWS = 2;
export const ONE_PAGE_TEXTAREA_MAX_ROWS = 6;

/** Rough characters per textarea row used to size fields that are limited by characters, not lines. */
export const ONE_PAGE_CHARS_PER_ROW = 90;

/** Delay after the last keystroke before edited values are re-measured on the server. */
export const ONE_PAGE_MEASURE_DEBOUNCE_MS = 500;

/** Reminder that the slide's hero photo is a placeholder the user must replace. */
export const ONE_PAGE_HOUSE_PHOTO_NOTICE =
    "The house photo on the slide is a placeholder — replace it with an image that fits the client after the deck is created.";

/** Help text under the dropzone. */
export const ONE_PAGE_SUPPORTED_FILES_HELP =
    "PDF, Word, PowerPoint, Excel, CSV, text, or screenshots (PNG/JPG/WebP) — up to 10 files";

/** Message shown when the one-page draft request fails without a usable response body. */
export const ONE_PAGE_DRAFT_FALLBACK_ERROR =
    "Could not draft from the uploaded files. Check the file types and try again.";

/** Message shown when one-page deck creation fails without a usable response body. */
export const ONE_PAGE_DECK_FALLBACK_ERROR = "Deck creation failed. Check Google access and try again.";

/**
 * Contract field names mapped to the labels the one-page builder shows, so a rejected request
 * names the input the user can actually see and fix.
 */
export const ONE_PAGE_FIELD_LABELS: Record<string, string> = {
    files: "Source files",
    fields: "Drafted fields",
    icons: "Icons",
    sourceFileNames: "Source files",
};
