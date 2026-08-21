import type { components } from "../../../shared/api/generated/schema";

/** RnD request as the API returns it. */
export type RndRequestV1 = components["schemas"]["RndRequestV1"];

/** Editable fields of the RnD request triage form. */
export interface FormState {
    title: string;
    requesterTeam: string;
    requestDetails: string;
    capabilityNotes: string;
    buyAmount: string;
}

/** Client-side validation messages keyed by the form field they belong to. */
export type FieldErrors = Partial<Record<keyof FormState, string>>;
