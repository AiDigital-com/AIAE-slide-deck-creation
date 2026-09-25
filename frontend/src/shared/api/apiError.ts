import type { ApiFieldError, ApiFieldLabels } from "./model/types";

// Jakarta's default @Size message. The contract's character budget means more to
// the user than the annotation's wording, so it is restated in plain English.
const SIZE_MESSAGE = /^size must be between \d+ and (\d+)$/;

/** Narrows an unknown error body to an indexable object. */
function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null;
}

/** Reads the field-level failures of a validation response, or an empty list. */
function readFieldErrors(body: Record<string, unknown>): ApiFieldError[] {
    const errors = body.errors;
    if (!Array.isArray(errors)) return [];
    return errors.filter(
        (entry): entry is ApiFieldError =>
            isRecord(entry) && typeof entry.field === "string" && typeof entry.error === "string",
    );
}

/** Restates a backend validation message in wording the form's user can act on. */
function humanizeFieldError(error: string): string {
    const size = SIZE_MESSAGE.exec(error);
    if (size) return `must be ${Number(size[1]).toLocaleString("en-US")} characters or fewer`;
    return error;
}

/** Reads the correlation id that ties the failure to its backend log entry. */
function readCorrelationId(body: Record<string, unknown>): string | null {
    return typeof body.correlationId === "string" && body.correlationId ? body.correlationId : null;
}

/**
 * Turns an `openapi-fetch` error body into a message that names the actual cause.
 *
 * Validation failures are reported per field under the label the form shows, so a
 * rejected brief says which input was refused and why. Application failures keep the
 * backend's own message and carry the correlation id for support. The fallback is used
 * only when the response carried no usable body, such as a network error or a 401.
 */
export function describeApiError(
    error: unknown,
    fallback: string,
    fieldLabels: ApiFieldLabels = {},
): string {
    if (!isRecord(error)) return fallback;

    const fieldErrors = readFieldErrors(error);
    if (fieldErrors.length > 0) {
        return fieldErrors
            .map((entry) => `${fieldLabels[entry.field] ?? entry.field}: ${humanizeFieldError(entry.error)}.`)
            .join(" ");
    }

    const message = typeof error.message === "string" ? error.message.trim() : "";
    const text = message || fallback;
    const correlationId = readCorrelationId(error);
    return correlationId ? `${text} (reference ${correlationId})` : text;
}
