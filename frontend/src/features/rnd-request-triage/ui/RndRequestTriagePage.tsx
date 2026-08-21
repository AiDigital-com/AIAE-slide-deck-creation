import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { AppShell } from "../../../app/AppShell";
import { apiClient } from "../../../shared/api/client";
import { ErrorAlert } from "../../../shared/ui/ErrorAlert";
import { LoadingBlock } from "../../../shared/ui/LoadingBlock";
import { EMPTY_FORM, USD } from "../constants/form";
import type { FieldErrors, FormState, RndRequestV1 } from "../model/types";
import "./rnd-request-triage.css";

/** RnD Request Triage page. */
export function RndRequestTriagePage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const [result, setResult] = useState<RndRequestV1 | null>(null);
    const [copied, setCopied] = useState(false);
    const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

    const buyAmountNumber = Number(form.buyAmount);
    const buyAmountValid = form.buyAmount.trim() !== "" && Number.isFinite(buyAmountNumber) && buyAmountNumber >= 0;

    const recentQuery = useQuery({
        queryKey: ["rnd-requests"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/rnd-requests");
            if (error) throw new Error("Failed to load recent requests");
            return data;
        },
    });

    const createMutation = useMutation({
        mutationFn: async (formData: FormState) => {
            const { data, error } = await apiClient.POST("/api/v1/rnd-requests", {
                body: {
                    title: formData.title,
                    requesterTeam: formData.requesterTeam || null,
                    requestDetails: formData.requestDetails || null,
                    capabilityNotes: formData.capabilityNotes || null,
                    buyAmount: Number(formData.buyAmount),
                },
            });
            if (error) throw new Error("Failed to triage request");
            return data;
        },
        onSuccess: (data) => {
            queryClient.invalidateQueries({ queryKey: ["rnd-requests"] });
            setResult(data ?? null);
            setCopied(false);
            setForm(EMPTY_FORM);
        },
    });

    function handleChange(field: keyof FormState, value: string) {
        setForm((prev) => ({ ...prev, [field]: value }));
        setFieldErrors((prev) => ({ ...prev, [field]: undefined }));
    }

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        const errors: FieldErrors = {};
        if (!form.title.trim()) {
            errors.title = "Enter a request title before triaging.";
        }
        if (!buyAmountValid) {
            errors.buyAmount = "Enter the deal size as a number of US dollars, zero or more.";
        }
        setFieldErrors(errors);
        if (errors.title || errors.buyAmount) {
            return;
        }
        setResult(null);
        createMutation.mutate(form);
    }

    async function handleCopy() {
        if (!result?.responseDraft) return;
        try {
            await navigator.clipboard.writeText(result.responseDraft);
            setCopied(true);
        } catch {
            setCopied(false);
        }
    }

    const escalated = result?.decision === "ESCALATE_TO_RND";

    return (
        <AppShell appName="Strategy & Planning">
            <div className="rrt">
                <div className="rrt__nav">
                    <button type="button" className="rrt__back" onClick={() => navigate("/")}>
                        ← Back
                    </button>
                    <span className="rrt__breadcrumb">Strategy / RnD Triage</span>
                </div>

                <h1 className="rrt__title">RnD Request Triage</h1>
                <p className="rrt__subtitle">
                    Triage growth-team requests by deal size. $100k+ escalates to RnD; smaller asks get a
                    positive, relay-ready workaround response.
                </p>

                <div className="rrt__connections" role="status">
                    <span className="rrt__connection">
                        <span className="rrt__connection-dot" aria-hidden="true" /> Slite: not connected
                    </span>
                    <span className="rrt__connection">
                        <span className="rrt__connection-dot" aria-hidden="true" /> Asana: not connected
                    </span>
                    <span className="rrt__connections-note">
                        Capability search activates once keys are added.
                    </span>
                </div>

                <div className="rrt__layout">
                    <div className="rrt__main">
                        <form className="rrt__form" onSubmit={handleSubmit}>
                            <div className="rrt__fields">
                                <div className="rrt__field">
                                    <label className="rrt__label" htmlFor="rrt-title">
                                        Request Title <span className="rrt__required">*</span>
                                    </label>
                                    <input
                                        id="rrt-title"
                                        type="text"
                                        className="rrt__input"
                                        value={form.title}
                                        onChange={(e) => handleChange("title", e.target.value)}
                                        placeholder="e.g. Real-time inventory sync for retail client"
                                        required
                                        aria-invalid={Boolean(fieldErrors.title)}
                                        aria-describedby={fieldErrors.title ? "rrt-title-error" : undefined}
                                    />
                                    {fieldErrors.title && (
                                        <p className="rrt__field-error" id="rrt-title-error" role="alert">
                                            {fieldErrors.title}
                                        </p>
                                    )}
                                </div>

                                <div className="rrt__field-row">
                                    <div className="rrt__field">
                                        <label className="rrt__label" htmlFor="rrt-team">Requesting Team</label>
                                        <input
                                            id="rrt-team"
                                            type="text"
                                            className="rrt__input"
                                            value={form.requesterTeam}
                                            onChange={(e) => handleChange("requesterTeam", e.target.value)}
                                            placeholder="e.g. Growth — EMEA"
                                        />
                                    </div>
                                    <div className="rrt__field">
                                        <label className="rrt__label" htmlFor="rrt-buy">
                                            Deal Size — “The Buy” (USD) <span className="rrt__required">*</span>
                                        </label>
                                        <input
                                            id="rrt-buy"
                                            type="number"
                                            min={0}
                                            step="1000"
                                            className="rrt__input"
                                            value={form.buyAmount}
                                            onChange={(e) => handleChange("buyAmount", e.target.value)}
                                            placeholder="e.g. 150000"
                                            required
                                            aria-invalid={Boolean(fieldErrors.buyAmount)}
                                            aria-describedby={fieldErrors.buyAmount ? "rrt-buy-error" : undefined}
                                        />
                                        {fieldErrors.buyAmount && (
                                            <p className="rrt__field-error" id="rrt-buy-error" role="alert">
                                                {fieldErrors.buyAmount}
                                            </p>
                                        )}
                                    </div>
                                </div>

                                <div className="rrt__field">
                                    <label className="rrt__label" htmlFor="rrt-details">Request Details</label>
                                    <textarea
                                        id="rrt-details"
                                        className="rrt__textarea"
                                        value={form.requestDetails}
                                        onChange={(e) => handleChange("requestDetails", e.target.value)}
                                        placeholder="What is the growth team asking for, and what does the client need?"
                                        rows={4}
                                    />
                                </div>

                                <div className="rrt__field">
                                    <label className="rrt__label" htmlFor="rrt-notes">Existing Capability Notes</label>
                                    <textarea
                                        id="rrt-notes"
                                        className="rrt__textarea"
                                        value={form.capabilityNotes}
                                        onChange={(e) => handleChange("capabilityNotes", e.target.value)}
                                        placeholder="Anything we can already do today that gets close? (Used in the workaround response.)"
                                        rows={3}
                                    />
                                </div>
                            </div>

                            {createMutation.isError && (
                                <ErrorAlert message="Failed to triage the request. Please try again." />
                            )}

                            <div className="rrt__actions">
                                <button
                                    type="submit"
                                    className="rrt__submit"
                                    disabled={!form.title.trim() || !buyAmountValid || createMutation.isPending}
                                >
                                    {createMutation.isPending ? "Triaging…" : "Triage Request"}
                                </button>
                            </div>
                        </form>

                        {result && (
                            <section
                                className={`rrt__result ${escalated ? "rrt__result--escalate" : "rrt__result--workaround"}`}
                                aria-live="polite"
                            >
                                <div className="rrt__result-head">
                                    <span
                                        className={`rrt__decision-badge ${escalated ? "rrt__decision-badge--escalate" : "rrt__decision-badge--workaround"}`}
                                    >
                                        {escalated ? "Valid RnD Escalation" : "Workaround Response"}
                                    </span>
                                    <span className="rrt__result-buy">
                                        Buy: {USD.format(result.buyAmount)}
                                    </span>
                                </div>
                                <p className="rrt__result-explain">
                                    {escalated
                                        ? "This deal meets the $100k threshold — submit the drafted summary below to RnD."
                                        : "Below the $100k threshold — relay the positive response below back to the growth team."}
                                </p>

                                {result.responseDraft && (
                                    <div className="rrt__draft">
                                        <div className="rrt__draft-head">
                                            <h2 className="rrt__draft-title">
                                                {escalated ? "Drafted RnD Summary" : "Relay-Ready Response"}
                                            </h2>
                                            <button type="button" className="rrt__copy" onClick={handleCopy}>
                                                {copied ? "Copied ✓" : "Copy"}
                                            </button>
                                        </div>
                                        <pre className="rrt__draft-body">{result.responseDraft}</pre>
                                    </div>
                                )}

                                {result.capabilitySummary && (
                                    <details className="rrt__capability">
                                        <summary className="rrt__capability-summary">Capability search status</summary>
                                        <pre className="rrt__capability-body">{result.capabilitySummary}</pre>
                                    </details>
                                )}
                            </section>
                        )}
                    </div>

                    <aside className="rrt__aside">
                        <div className="rrt__recent">
                            <h2 className="rrt__recent-title">Recent Requests</h2>
                            {recentQuery.isLoading && <LoadingBlock />}
                            {recentQuery.isError && (
                                <p className="rrt__recent-empty">Could not load recent requests.</p>
                            )}
                            {recentQuery.data?.length === 0 && (
                                <p className="rrt__recent-empty">No triaged requests yet.</p>
                            )}
                            {recentQuery.data && recentQuery.data.length > 0 && (
                                <ul className="rrt__recent-list">
                                    {recentQuery.data.map((req) => (
                                        <li key={req.id} className="rrt__recent-item">
                                            <span className="rrt__recent-name">{req.title}</span>
                                            <span className="rrt__recent-meta">{USD.format(req.buyAmount)}</span>
                                            <span
                                                className={`rrt__recent-badge ${
                                                    req.decision === "ESCALATE_TO_RND"
                                                        ? "rrt__recent-badge--escalate"
                                                        : "rrt__recent-badge--workaround"
                                                }`}
                                            >
                                                {req.decision === "ESCALATE_TO_RND" ? "Escalated" : "Workaround"}
                                            </span>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    </aside>
                </div>
            </div>
        </AppShell>
    );
}
