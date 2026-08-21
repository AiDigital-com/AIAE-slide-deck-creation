import type { FormState } from "../model/types";

/** A blank triage form — the initial state and the post-submit reset value. */
export const EMPTY_FORM: FormState = {
    title: "",
    requesterTeam: "",
    requestDetails: "",
    capabilityNotes: "",
    buyAmount: "",
};

/** Whole-dollar USD formatter used for the buy amount shown on triage results. */
export const USD = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
});
