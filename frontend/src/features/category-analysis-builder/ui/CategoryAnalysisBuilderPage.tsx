import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { useClerk } from "@clerk/clerk-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { AppShell } from "../../../app/AppShell";
import { apiClient } from "../../../shared/api/client";
import { ErrorAlert } from "../../../shared/ui/ErrorAlert";
import { LoadingBlock } from "../../../shared/ui/LoadingBlock";
import { EMPTY_BRIEF, SLIDE_TITLES } from "../constants/brief";
import type {
    BriefFieldErrors,
    BriefState,
    DraftAlignment,
    GoogleStatus,
    StandardDraftField,
    Step,
} from "../model/types";
import { mergeSlideRedraft } from "./redraftMerge";
import "./category-analysis-builder.css";

/**
 * Clerk-backed re-consent action. MISSING_SCOPES opens the connected-accounts manager to
 * re-grant Drive/Slides; NOT_CONNECTED signs the user out so they can sign back in with Google.
 */
function GoogleReconnectAction({ status }: { status: GoogleStatus }) {
    const clerk = useClerk();
    if (status === "MISSING_SCOPES") {
        return (
            <button
                type="button"
                className="cab__notice-action"
                onClick={() => clerk.openUserProfile()}
            >
                Reconnect Google
            </button>
        );
    }
    return (
        <button
            type="button"
            className="cab__notice-action"
            onClick={() => clerk.signOut({ redirectUrl: "/login" })}
        >
            Sign in with Google
        </button>
    );
}

/**
 * Renders the targeted fix-it notice for the user's Google connection state.
 * Shows nothing when Drive/Slides access is already granted.
 */
function GoogleAccessNotice({ status }: { status: GoogleStatus }) {
    if (status === "CONNECTED") {
        return null;
    }
    const notConnected = status === "NOT_CONNECTED";
    return (
        <div className="cab__notice">
            <p className="cab__notice-title">
                {notConnected
                    ? "You're not signed in with Google"
                    : "Google Drive & Slides access wasn't granted"}
            </p>
            <p className="cab__notice-body">
                {notConnected
                    ? "Deck creation happens in your own Google Drive. Sign in with your Google account to continue."
                    : "You're signed in with Google, but Drive & Slides access wasn't allowed. Reconnect Google and allow Drive & Slides access. If it keeps failing, ask your admin to enable the Drive & Slides scopes on the Google connection."}
            </p>
            <GoogleReconnectAction status={status} />
        </div>
    );
}

/** Category Analysis Builder — AI-drafts the Standard 5-slide deck and creates it in Google Slides. */
export function CategoryAnalysisBuilderPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [step, setStep] = useState<Step>("brief");
    const [brief, setBrief] = useState<BriefState>(EMPTY_BRIEF);
    const [briefErrors, setBriefErrors] = useState<BriefFieldErrors>({});
    const [fields, setFields] = useState<StandardDraftField[]>([]);
    const [alignment, setAlignment] = useState<DraftAlignment | null>(null);
    const [slidesUrl, setSlidesUrl] = useState<string | null>(null);
    const [openNoteSlide, setOpenNoteSlide] = useState<number | null>(null);
    const [slideNotes, setSlideNotes] = useState<Record<number, string>>({});
    const [activeRedraftSlide, setActiveRedraftSlide] = useState<number | null>(null);
    const [updatedSlide, setUpdatedSlide] = useState<number | null>(null);
    const [updateTick, setUpdateTick] = useState(0);

    const connectionsQuery = useQuery({
        queryKey: ["category-analyses", "standard-connections"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET(
                "/api/v1/category-analyses/standard-connections",
            );
            if (error) throw new Error("Failed to check connections");
            return data;
        },
    });

    const recentQuery = useQuery({
        queryKey: ["category-analyses"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/category-analyses");
            if (error) throw new Error("Failed to load recent analyses");
            return data;
        },
    });

    const draftMutation = useMutation({
        mutationFn: async (input: BriefState) => {
            const { data, error } = await apiClient.POST(
                "/api/v1/category-analyses/standard-drafts",
                {
                    body: {
                        category: input.category,
                        clientName: input.clientName,
                        clientWebsite: input.clientWebsite,
                        storyTheme: input.storyTheme || null,
                        guidanceNotes: input.guidanceNotes || null,
                    },
                },
            );
            if (error || !data) throw new Error("Drafting failed");
            return data;
        },
        onSuccess: (data) => {
            setFields(data.fields);
            setAlignment(data.alignment);
            setStep("review");
        },
    });

    const redraftMutation = useMutation({
        mutationFn: async ({ slideNumber, note }: { slideNumber: number; note: string }) => {
            const { data, error } = await apiClient.POST(
                "/api/v1/category-analyses/standard-drafts/slide",
                {
                    body: {
                        category: brief.category,
                        clientName: brief.clientName,
                        guidanceNotes: brief.guidanceNotes || null,
                        slideNumber,
                        slideNote: note.trim() || null,
                        currentFields: fields.map((f) => ({ key: f.key, value: f.value })),
                    },
                },
            );
            if (error || !data) throw new Error("Redraft failed");
            return { data, slideNumber };
        },
        onSuccess: ({ data, slideNumber }) => {
            const merged = mergeSlideRedraft(fields, data);
            setFields(merged.fields);
            setOpenNoteSlide(null);
            setSlideNotes((prev) => ({ ...prev, [slideNumber]: "" }));
            setActiveRedraftSlide(null);
            setUpdatedSlide(slideNumber);
            setUpdateTick((t) => t + 1);
        },
    });

    const deckMutation = useMutation({
        mutationFn: async () => {
            const { data, error } = await apiClient.POST(
                "/api/v1/category-analyses/standard-decks",
                {
                    body: {
                        category: brief.category,
                        clientName: brief.clientName,
                        fields: fields.map((f) => ({ key: f.key, value: f.value })),
                    },
                },
            );
            if (error || !data) throw new Error("Deck creation failed");
            return data;
        },
        onSuccess: (data) => {
            setSlidesUrl(data.slidesUrl ?? null);
            setUpdatedSlide(null);
            setStep("done");
            queryClient.invalidateQueries({ queryKey: ["category-analyses"] });
        },
    });

    const aiConnected = connectionsQuery.data?.aiConnected ?? false;
    const googleStatus: GoogleStatus = connectionsQuery.data?.googleStatus ?? "NOT_CONNECTED";
    const googleConnected = googleStatus === "CONNECTED";

    function handleBriefChange(field: keyof BriefState, value: string) {
        setBrief((prev) => ({ ...prev, [field]: value }));
        setBriefErrors((prev) => ({ ...prev, [field]: undefined }));
    }

    function handleDraftSubmit(e: React.FormEvent) {
        e.preventDefault();
        const errors: BriefFieldErrors = {};
        if (!brief.category.trim()) {
            errors.category = "Name the category the deck should analyse.";
        }
        if (!brief.clientName.trim()) {
            errors.clientName = "Name the client the deck is for.";
        }
        if (!brief.clientWebsite.trim()) {
            errors.clientWebsite = "Add the client's website so the draft stays relevant to their business.";
        }
        setBriefErrors(errors);
        if (errors.category || errors.clientName || errors.clientWebsite) {
            return;
        }
        draftMutation.mutate(brief);
    }

    function handleFieldChange(key: string, value: string) {
        setUpdatedSlide(null);
        setFields((prev) => prev.map((f) => (f.key === key ? { ...f, value } : f)));
    }

    function handleStartOver() {
        setStep("brief");
        setBrief(EMPTY_BRIEF);
        setFields([]);
        setAlignment(null);
        setSlidesUrl(null);
        setOpenNoteSlide(null);
        setSlideNotes({});
        setActiveRedraftSlide(null);
        setUpdatedSlide(null);
        draftMutation.reset();
        deckMutation.reset();
        redraftMutation.reset();
    }

    function handleOpenNote(slide: number) {
        redraftMutation.reset();
        setActiveRedraftSlide(null);
        setUpdatedSlide(null);
        setOpenNoteSlide(slide);
    }

    function handleRedraft(slide: number) {
        setActiveRedraftSlide(slide);
        redraftMutation.mutate({ slideNumber: slide, note: slideNotes[slide] ?? "" });
    }

    const slideNumbers = [...new Set(fields.map((f) => f.slideNumber))].sort((a, b) => a - b);

    return (
        <AppShell appName="Strategy & Planning">
            <div className="cab">
                <div className="cab__nav">
                    <button type="button" className="cab__back" onClick={() => navigate("/")}>
                        ← Back
                    </button>
                    <span className="cab__breadcrumb">Strategy / Category Analysis</span>
                </div>

                <h1 className="cab__title">Category Analysis Builder</h1>
                <p className="cab__subtitle">
                    Describe the category and client, review the AI-drafted content, and get a
                    ready-to-edit 5-slide deck in your own Google Slides.
                </p>

                {connectionsQuery.isSuccess && (
                    <div className="cab__connections">
                        <span
                            className={`cab__conn ${aiConnected ? "cab__conn--ok" : "cab__conn--off"}`}
                        >
                            {aiConnected ? "✓ AI drafting connected" : "✕ AI drafting not connected"}
                        </span>
                        <span
                            className={`cab__conn ${googleConnected ? "cab__conn--ok" : "cab__conn--off"}`}
                        >
                            {googleConnected && "✓ Google Slides connected"}
                            {googleStatus === "MISSING_SCOPES" && "✕ Google Drive & Slides access not granted"}
                            {googleStatus === "NOT_CONNECTED" && "✕ Not signed in with Google"}
                        </span>
                    </div>
                )}

                <div className="cab__layout">
                    <div className="cab__main">
                        {step === "brief" && (
                            <form className="cab__form" onSubmit={handleDraftSubmit}>
                                {connectionsQuery.isSuccess && !aiConnected && (
                                    <div className="cab__notice">
                                        <p className="cab__notice-title">AI drafting is not connected</p>
                                        <p className="cab__notice-body">
                                            Ask your admin to add the OpenAI API key so the builder can
                                            draft slide content for you.
                                        </p>
                                    </div>
                                )}

                                <div className="cab__fields">
                                    <div className="cab__field">
                                        <label className="cab__label" htmlFor="cab-category">
                                            Category <span className="cab__required">*</span>
                                        </label>
                                        <input
                                            id="cab-category"
                                            type="text"
                                            className="cab__input"
                                            value={brief.category}
                                            onChange={(e) => handleBriefChange("category", e.target.value)}
                                            aria-invalid={Boolean(briefErrors.category)}
                                            aria-describedby={briefErrors.category ? "cab-category-error" : undefined}
                                            placeholder="e.g. Regional Travel, Western Boots, Medicare Advantage"
                                            required
                                        />
                                        {briefErrors.category && (
                                            <p className="cab__field-error" id="cab-category-error" role="alert">
                                                {briefErrors.category}
                                            </p>
                                        )}
                                    </div>

                                    <div className="cab__field">
                                        <label className="cab__label" htmlFor="cab-client">
                                            Client <span className="cab__required">*</span>
                                        </label>
                                        <input
                                            id="cab-client"
                                            type="text"
                                            className="cab__input"
                                            value={brief.clientName}
                                            onChange={(e) => handleBriefChange("clientName", e.target.value)}
                                            aria-invalid={Boolean(briefErrors.clientName)}
                                            aria-describedby={briefErrors.clientName ? "cab-client-error" : undefined}
                                            placeholder="e.g. Alberta Boot"
                                            required
                                        />
                                        {briefErrors.clientName && (
                                            <p className="cab__field-error" id="cab-client-error" role="alert">
                                                {briefErrors.clientName}
                                            </p>
                                        )}
                                    </div>

                                    <div className="cab__field">
                                        <label className="cab__label" htmlFor="cab-website">
                                            Client website <span className="cab__required">*</span>
                                        </label>
                                        <input
                                            id="cab-website"
                                            type="url"
                                            className="cab__input"
                                            value={brief.clientWebsite}
                                            onChange={(e) => handleBriefChange("clientWebsite", e.target.value)}
                                            aria-invalid={Boolean(briefErrors.clientWebsite)}
                                            aria-describedby={briefErrors.clientWebsite ? "cab-website-error" : undefined}
                                            placeholder="e.g. https://albertaboot.com"
                                            required
                                        />
                                        {briefErrors.clientWebsite && (
                                            <p className="cab__field-error" id="cab-website-error" role="alert">
                                                {briefErrors.clientWebsite}
                                            </p>
                                        )}
                                        <p className="cab__hint">
                                            We review the site to keep the deck relevant to the client's business
                                            and confirm the topic matches what they do.
                                        </p>
                                    </div>

                                    <div className="cab__field">
                                        <label className="cab__label" htmlFor="cab-theme">Story theme</label>
                                        <textarea
                                            id="cab-theme"
                                            className="cab__textarea"
                                            value={brief.storyTheme}
                                            onChange={(e) => handleBriefChange("storyTheme", e.target.value)}
                                            placeholder="Optional narrative to guide the arc, e.g. “how connected TV can grow market awareness”…"
                                            rows={2}
                                        />
                                    </div>

                                    <div className="cab__field">
                                        <label className="cab__label" htmlFor="cab-notes">Focus notes</label>
                                        <textarea
                                            id="cab-notes"
                                            className="cab__textarea"
                                            value={brief.guidanceNotes}
                                            onChange={(e) => handleBriefChange("guidanceNotes", e.target.value)}
                                            placeholder="Optional angles, audiences, or constraints to steer the draft…"
                                            rows={3}
                                        />
                                    </div>
                                </div>

                                {draftMutation.isError && (
                                    <ErrorAlert message="Drafting failed. Check the AI connection and try again." />
                                )}

                                <button
                                    type="submit"
                                    className="cab__submit"
                                    disabled={
                                        !aiConnected ||
                                        !brief.category.trim() ||
                                        !brief.clientName.trim() ||
                                        !brief.clientWebsite.trim() ||
                                        draftMutation.isPending
                                    }
                                >
                                    {draftMutation.isPending ? "Drafting content…" : "Draft slide content"}
                                </button>
                            </form>
                        )}

                        {step === "review" && (
                            <div className="cab__form">
                                <div className="cab__review-head">
                                    <h2 className="cab__review-title">Review the draft</h2>
                                    <p className="cab__review-sub">
                                        Edit anything below. Stats and sources are AI-drafted — verify them
                                        before sharing with the client.
                                    </p>
                                </div>

                                {alignment && (
                                    <div
                                        className={`cab__alignment ${
                                            !alignment.confirmed
                                                ? "cab__alignment--unknown"
                                                : alignment.matches
                                                  ? "cab__alignment--ok"
                                                  : "cab__alignment--warn"
                                        }`}
                                    >
                                        <span className="cab__alignment-icon" aria-hidden="true">
                                            {!alignment.confirmed ? "?" : alignment.matches ? "✓" : "!"}
                                        </span>
                                        <div>
                                            <p className="cab__alignment-title">
                                                {!alignment.confirmed
                                                    ? "Couldn't confirm alignment"
                                                    : alignment.matches
                                                      ? "Aligned with the client's business"
                                                      : "Check topic vs. the client's business"}
                                            </p>
                                            <p className="cab__alignment-body">{alignment.message}</p>
                                            {alignment.clientBusinessFocus && (
                                                <p className="cab__alignment-focus">
                                                    Client focus: {alignment.clientBusinessFocus}
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                )}

                                {slideNumbers.map((slide) => (
                                    <div
                                        className={`cab__slide-group ${
                                            updatedSlide === slide ? "cab__slide-group--updated" : ""
                                        }`}
                                        key={slide}
                                    >
                                        <div className="cab__slide-head">
                                            <h3 className="cab__slide-title">
                                                {SLIDE_TITLES[slide] ?? `Slide ${slide}`}
                                            </h3>
                                            {updatedSlide === slide && (
                                                <span
                                                    className="cab__slide-updated"
                                                    role="status"
                                                    aria-live="polite"
                                                    key={updateTick}
                                                >
                                                    ✓ Updated just now
                                                </span>
                                            )}
                                        </div>
                                        {fields
                                            .filter((f) => f.slideNumber === slide)
                                            .map((field) => (
                                                <div className="cab__field" key={field.key}>
                                                    <label className="cab__label" htmlFor={`cab-f-${field.key}`}>
                                                        {field.label}
                                                    </label>
                                                    {field.value.length > 60 ? (
                                                        <textarea
                                                            id={`cab-f-${field.key}`}
                                                            className="cab__textarea"
                                                            value={field.value}
                                                            rows={2}
                                                            onChange={(e) => handleFieldChange(field.key, e.target.value)}
                                                        />
                                                    ) : (
                                                        <input
                                                            id={`cab-f-${field.key}`}
                                                            type="text"
                                                            className="cab__input"
                                                            value={field.value}
                                                            onChange={(e) => handleFieldChange(field.key, e.target.value)}
                                                        />
                                                    )}
                                                </div>
                                            ))}

                                        <div className="cab__slide-actions">
                                            {openNoteSlide === slide ? (
                                                <div className="cab__redraft">
                                                    <label
                                                        className="cab__label"
                                                        htmlFor={`cab-redraft-${slide}`}
                                                    >
                                                        Redraft guidance (optional)
                                                    </label>
                                                    <textarea
                                                        id={`cab-redraft-${slide}`}
                                                        className="cab__textarea"
                                                        placeholder="e.g. make it more data-driven, focus on Gen Z, punchier headline…"
                                                        rows={2}
                                                        value={slideNotes[slide] ?? ""}
                                                        onChange={(e) =>
                                                            setSlideNotes((p) => ({ ...p, [slide]: e.target.value }))
                                                        }
                                                        disabled={
                                                            redraftMutation.isPending &&
                                                            activeRedraftSlide === slide
                                                        }
                                                    />
                                                    <div className="cab__redraft-actions">
                                                        <button
                                                            type="button"
                                                            className="cab__secondary"
                                                            onClick={() => setOpenNoteSlide(null)}
                                                            disabled={
                                                                redraftMutation.isPending &&
                                                                activeRedraftSlide === slide
                                                            }
                                                        >
                                                            Cancel
                                                        </button>
                                                        <button
                                                            type="button"
                                                            className="cab__redraft-go"
                                                            onClick={() => handleRedraft(slide)}
                                                            disabled={!aiConnected || redraftMutation.isPending}
                                                        >
                                                            {redraftMutation.isPending &&
                                                            activeRedraftSlide === slide
                                                                ? "Redrafting…"
                                                                : "Redraft slide"}
                                                        </button>
                                                    </div>
                                                </div>
                                            ) : (
                                                <button
                                                    type="button"
                                                    className="cab__redraft-toggle"
                                                    onClick={() => handleOpenNote(slide)}
                                                    disabled={!aiConnected || redraftMutation.isPending}
                                                >
                                                    ↻ Redraft this slide
                                                </button>
                                            )}
                                            {redraftMutation.isError && activeRedraftSlide === slide && (
                                                <ErrorAlert message="Redraft failed. Check the AI connection and try again." />
                                            )}
                                        </div>
                                    </div>
                                ))}

                                <GoogleAccessNotice status={googleStatus} />

                                {deckMutation.isError && (
                                    <ErrorAlert message="Deck creation failed. Check Google access and try again." />
                                )}

                                <div className="cab__actions">
                                    <button type="button" className="cab__secondary" onClick={handleStartOver}>
                                        Start over
                                    </button>
                                    <button
                                        type="button"
                                        className="cab__submit"
                                        disabled={
                                            !googleConnected ||
                                            deckMutation.isPending ||
                                            redraftMutation.isPending
                                        }
                                        onClick={() => deckMutation.mutate()}
                                    >
                                        {deckMutation.isPending ? "Creating deck…" : "Create Google Slides deck"}
                                    </button>
                                </div>
                            </div>
                        )}

                        {step === "done" && (
                            <div className="cab__form">
                                <div className="cab__success">
                                    <span className="cab__success-icon" aria-hidden="true">✓</span>
                                    <div>
                                        <p className="cab__success-title">Deck created!</p>
                                        <p className="cab__success-body">
                                            Your Category Analysis deck is ready in your Google Drive.
                                        </p>
                                        {slidesUrl && (
                                            <a
                                                className="cab__slides-link"
                                                href={slidesUrl}
                                                target="_blank"
                                                rel="noreferrer"
                                            >
                                                Open in Google Slides →
                                            </a>
                                        )}
                                    </div>
                                </div>
                                <button type="button" className="cab__secondary" onClick={handleStartOver}>
                                    Create another analysis
                                </button>
                            </div>
                        )}
                    </div>

                    <aside className="cab__aside">
                        <div className="cab__engine">
                            <h2 className="cab__engine-title">How it works</h2>
                            <p className="cab__engine-body">
                                1. Brief the builder on the category and client.
                                <br />
                                2. AI drafts every slide field — you review and edit.
                                <br />
                                3. The deck is created from the agency master template in your own
                                Google Slides.
                            </p>
                        </div>

                        <div className="cab__recent">
                            <h2 className="cab__recent-title">Recent Analyses</h2>
                            {recentQuery.isLoading && <LoadingBlock />}
                            {recentQuery.isError && (
                                <p className="cab__recent-empty">Could not load recent analyses.</p>
                            )}
                            {recentQuery.data?.length === 0 && (
                                <p className="cab__recent-empty">No recent analyses.</p>
                            )}
                            {recentQuery.data && recentQuery.data.length > 0 && (
                                <ul className="cab__recent-list">
                                    {recentQuery.data.map((item) => (
                                        <li key={item.id} className="cab__recent-item">
                                            <span className="cab__recent-name">{item.title}</span>
                                            {item.category && (
                                                <span className="cab__recent-meta">{item.category}</span>
                                            )}
                                            {item.slidesUrl ? (
                                                <a
                                                    className="cab__recent-link"
                                                    href={item.slidesUrl}
                                                    target="_blank"
                                                    rel="noreferrer"
                                                >
                                                    Open deck →
                                                </a>
                                            ) : (
                                                <span
                                                    className={`cab__recent-badge cab__recent-badge--${item.status?.toLowerCase()}`}
                                                >
                                                    {item.status === "SUBMITTED" ? "Submitted" : "Draft"}
                                                </span>
                                            )}
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
