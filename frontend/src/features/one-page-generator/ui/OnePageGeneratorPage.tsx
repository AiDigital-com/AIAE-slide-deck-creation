import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { useMutation, useQuery } from "@tanstack/react-query";

import { AppShell } from "../../../app/AppShell";
import { describeApiError } from "../../../shared/api/apiError";
import { apiClient } from "../../../shared/api/client";
import {
    ONE_PAGER_CONNECTIONS_QUERY_KEY,
    ONE_PAGER_DECK_FALLBACK_ERROR,
    ONE_PAGER_DRAFT_FALLBACK_ERROR,
    ONE_PAGER_FIELD_LABELS,
} from "../constants/form";
import { EMPTY_BRIEF } from "../constants/modules";
import { buildCreateRequest, buildDraftRequest, toggleModule, validateBrief } from "../model/moduleOrder";
import type { BriefErrors, BriefState, BriefTextField, BuilderStep, GoogleStatus, ModuleKey, OnePagerDraft, ShellVariant } from "../model/types";
import { BriefForm } from "./BriefForm";
import { DoneStep } from "./DoneStep";
import { DraftReview } from "./DraftReview";
import { LiveOutline } from "./LiveOutline";
import "./one-page-generator.css";

/**
 * One Page Generator: pick a shell and body modules, brief the drafter, review the drafted copy,
 * and get a single-slide page assembled from the master template in your own Google Slides.
 */
export function OnePageGeneratorPage() {
    const navigate = useNavigate();
    const [step, setStep] = useState<BuilderStep>("setup");
    const [brief, setBrief] = useState<BriefState>(EMPTY_BRIEF);
    const [errors, setErrors] = useState<BriefErrors>({});
    const [draft, setDraft] = useState<OnePagerDraft | null>(null);
    const [slidesUrl, setSlidesUrl] = useState<string | null>(null);

    const connectionsQuery = useQuery({
        queryKey: ONE_PAGER_CONNECTIONS_QUERY_KEY,
        queryFn: async ({ signal }) => {
            const { data, error } = await apiClient.GET("/api/v1/one-pagers/connections", { signal });
            if (error || !data) throw new Error("Failed to check connections");
            return data;
        },
    });

    const draftMutation = useMutation({
        mutationFn: async (input: BriefState) => {
            const { data, error } = await apiClient.POST("/api/v1/one-pagers/drafts", { body: buildDraftRequest(input) });
            if (error || !data) {
                throw new Error(describeApiError(error, ONE_PAGER_DRAFT_FALLBACK_ERROR, ONE_PAGER_FIELD_LABELS));
            }
            return data;
        },
        onSuccess: (data) => {
            setDraft(data);
            setStep("review");
            window.scrollTo({ top: 0, behavior: "smooth" });
        },
    });

    const deckMutation = useMutation({
        mutationFn: async (content: OnePagerDraft) => {
            const { data, error } = await apiClient.POST("/api/v1/one-pagers/decks", { body: buildCreateRequest(brief, content) });
            if (error || !data) {
                throw new Error(describeApiError(error, ONE_PAGER_DECK_FALLBACK_ERROR, ONE_PAGER_FIELD_LABELS));
            }
            return data;
        },
        onSuccess: (data) => {
            setSlidesUrl(data.slidesUrl);
            setStep("done");
            // Finishing while scrolled down a long review would leave the confirmation off-screen.
            window.scrollTo({ top: 0, behavior: "smooth" });
        },
    });

    const aiConnected = connectionsQuery.data?.aiConnected ?? false;
    const googleStatus: GoogleStatus = connectionsQuery.data?.googleStatus ?? "NOT_CONNECTED";
    const googleConnected = googleStatus === "CONNECTED";

    function handleBriefChange<K extends BriefTextField>(field: K, value: BriefState[K]) {
        setBrief((prev) => ({ ...prev, [field]: value }));
        setErrors((prev) => (prev[field] ? { ...prev, [field]: undefined } : prev));
    }

    function handleShellChange(shellVariant: ShellVariant) {
        setBrief((prev) => ({ ...prev, shellVariant }));
    }

    function handleModuleToggle(key: ModuleKey, active: boolean) {
        setBrief((prev) => ({ ...prev, moduleOrder: toggleModule(prev.moduleOrder, key, active) }));
    }

    function handleReorder(moduleOrder: ModuleKey[]) {
        setBrief((prev) => ({ ...prev, moduleOrder }));
    }

    function handleDraftSubmit() {
        if (draftMutation.isPending) return;
        const validation = validateBrief(brief);
        setErrors(validation);
        if (Object.keys(validation).length > 0) return;
        draftMutation.mutate(brief);
    }

    function handleStartOver() {
        setStep("setup");
        setBrief(EMPTY_BRIEF);
        setErrors({});
        setDraft(null);
        setSlidesUrl(null);
        draftMutation.reset();
        deckMutation.reset();
    }

    return (
        <AppShell appName="Strategy Toolkit">
            <div className="opg">
                <div className="opg__nav">
                    <button type="button" className="opg__back" onClick={() => navigate("/")}>
                        ← Back
                    </button>
                    <span className="opg__breadcrumb">Strategy / One Page Generator</span>
                </div>

                <h1 className="opg__title">One Page Generator</h1>
                <p className="opg__subtitle">
                    Pick a header/footer shell and the body modules you want, brief the builder, review the draft, and
                    get a single-slide page assembled from the master template in your own Google Slides.
                </p>

                {connectionsQuery.isSuccess && (
                    <div className="opg__connections">
                        <span className={`opg__conn ${aiConnected ? "opg__conn--ok" : "opg__conn--off"}`}>
                            {aiConnected ? "✓ AI drafting connected" : "✕ AI drafting not connected"}
                        </span>
                        <span className={`opg__conn ${googleConnected ? "opg__conn--ok" : "opg__conn--off"}`}>
                            {googleConnected && "✓ Google Slides connected"}
                            {googleStatus === "MISSING_SCOPES" && "✕ Google Drive & Slides access not granted"}
                            {googleStatus === "NOT_CONNECTED" && "✕ Not signed in with Google"}
                        </span>
                    </div>
                )}

                <div className={`opg__layout${step === "setup" ? " opg__layout--with-outline" : ""}`}>
                    <div className="opg__main">
                        {step === "setup" && (
                            <BriefForm
                                brief={brief}
                                errors={errors}
                                aiConnected={aiConnected}
                                connectionsLoaded={connectionsQuery.isSuccess}
                                drafting={draftMutation.isPending}
                                draftError={draftMutation.isError ? draftMutation.error.message : null}
                                onBriefChange={handleBriefChange}
                                onShellChange={handleShellChange}
                                onModuleToggle={handleModuleToggle}
                                onSubmit={handleDraftSubmit}
                            />
                        )}

                        {step === "review" && draft && (
                            <DraftReview
                                draft={draft}
                                googleStatus={googleStatus}
                                connectionsLoaded={connectionsQuery.isSuccess}
                                creating={deckMutation.isPending}
                                createError={deckMutation.isError ? deckMutation.error.message : null}
                                onChange={setDraft}
                                onStartOver={handleStartOver}
                                onCreate={() => deckMutation.mutate(draft)}
                            />
                        )}

                        {step === "done" && <DoneStep slidesUrl={slidesUrl} onStartOver={handleStartOver} />}
                    </div>

                    {step === "setup" && (
                        <aside className="opg__aside">
                            <LiveOutline
                                brief={brief}
                                onReorder={handleReorder}
                                onRemove={(key) => handleModuleToggle(key, false)}
                                onAddSolutionsVisual={() => handleModuleToggle("SOLUTIONS_VISUAL", true)}
                            />
                        </aside>
                    )}

                    <aside className="opg__aside opg__aside--how">
                        <div className="opg__aside-card">
                            <h2 className="opg__aside-title">How it works</h2>
                            <ol className="opg__steps">
                                <li className={`opg__step${step === "setup" ? " opg__step--active" : ""}`}>
                                    Pick one header/footer shell and the body modules you want; every module is a known-good,
                                    pre-built layout. Arrange them in the live outline.
                                </li>
                                <li className={`opg__step${step === "review" ? " opg__step--active" : ""}`}>
                                    Paste a guidance brief with every fact the page should carry. AI fits it into the modules you
                                    picked, never researching or inventing claims, and you review and edit.
                                </li>
                                <li className={`opg__step${step === "done" ? " opg__step--active" : ""}`}>
                                    The page is assembled from the master template and saved as a new file in your own Google
                                    Slides.
                                </li>
                            </ol>
                        </div>
                    </aside>
                </div>
            </div>
        </AppShell>
    );
}
