import type { components } from "../generated/schema";

/** Body returned with a 400 when request validation fails. */
export type ApiValidationResponse = components["schemas"]["AppValidationExceptionResponseV1"];

/** Body returned with an application failure such as 403, 500, or 502. */
export type ApiFailureResponse = components["schemas"]["AppApiExceptionResponseV1"];

/** One field-level validation failure inside {@link ApiValidationResponse}. */
export type ApiFieldError = components["schemas"]["FieldToErrorResponseV1"];

/** Contract field names mapped to the labels the user sees on the form. */
export type ApiFieldLabels = Readonly<Record<string, string>>;
