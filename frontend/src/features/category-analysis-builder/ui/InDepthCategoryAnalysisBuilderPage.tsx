import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { useClerk } from "@clerk/clerk-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { AppShell } from "../../../app/AppShell";
import { describeApiError } from "../../../shared/api/apiError";
import { apiClient } from "../../../shared/api/client";
import { ErrorAlert } from "../../../shared/ui/ErrorAlert";
import { LoadingBlock } from "../../../shared/ui/LoadingBlock";
import {
    EMPTY_IN_DEPTH_BRIEF,
    IN_DEPTH_CATALOG_ERROR,
    IN_DEPTH_DECK_FALLBACK_ERROR,
    IN_DEPTH_DRAFT_FALLBACK_ERROR,
    IN_DEPTH_FIELD_LABELS,
    IN_DEPTH_LONG_VALUE_THRESHOLD,
    IN_DEPTH_REDRAFT_FALLBACK_ERROR,
    IN_DEPTH_SLIDE_PREVIEW_PATH,
    IN_DEPTH_TITLE_VARIANT_SEPARATOR,
} from "../constants/inDepth";
import { CATEGORY_ANALYSIS_PICKER_ROUTE } from "../constants/templates";
import type {
    DraftAlignment,
    GoogleStatus,
    InDepthBriefFieldErrors,
    InDepthBriefState,
    InDepthDraftField,
    InDepthSlide,
    InDepthStep,
} from "../model/types";
import { mergeInDepthSlideRedraft } from "./inDepthRedraftMerge";
import { selectInDepthAnalyses } from "../model/recentAnalyses";
import "./category-analysis-builder.css";

/**
 * Clerk-backed re-consent action, mirroring the Standard flow's — the same Clerk Google
 * connection backs both templates.
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

/** Short topic label for a slide's checklist title, stripping the "— layout X" variant suffix. */
function baseTopic(title: string): string {
    return title.split(IN_DEPTH_TITLE_VARIANT_SEPARATOR)[0];
}

/**
 * In-Depth Category Analysis Builder — the user checks which slides of the 24-slide master
 * template to include, AI-drafts just those (grounded in live web search), then creates the
 * deck in Google Slides with every unchecked slide removed from the copied template.
 */
export function InDepthCategoryAnalysisBuilderPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [step, setStep] = useState<InDepthStep>("setup");
    const [brief, setBrief] = useState<InDepthBriefState>(EMPTY_IN_DEPTH_BRIEF);
    const [briefErrors, setBriefErrors] = useState<InDepthBriefFieldErrors>({});
    const [selected, setSelected] = useState<Set<number>>(new Set());
    const [selectionInitialized, setSelectionInitialized] = useState(false);
    const [fields, setFields] = useState<InDepthDraftField[]>([]);
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

    const slidesQuery = useQuery({
        queryKey: ["category-analyses", "in-depth-slides"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/category-analyses/in-depth-slides");
            if (error || !data) throw new Error(IN_DEPTH_CATALOG_ERROR);
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
        select: selectInDepthAnalyses,
    });

    // Seed the checklist from each slide's default once, when the catalog first loads.
    useEffect(() => {
        if (!selectionInitialized && slidesQuery.data) {
            setSelected(new Set(slidesQuery.data.filter((s) => s.defaultChecked).map((s) => s.slideNumber)));
            setSelectionInitialized(true);
        }
    }, [selectionInitialized, slidesQuery.data]);

    const draftMutation = useMutation({
        mutationFn: async (input: { brief: InDepthBriefState; selectedSlides: number[] }) => {
            const { data, error } = await apiClient.POST("/api/v1/category-analyses/in-depth-drafts", {
                body: {
                    category: input.brief.category,
                    clientName: input.brief.clientName,
                    clientWebsite: input.brief.clientWebsite,
                    guidanceNotes: input.brief.guidanceNotes || null,
                    selectedSlides: input.selectedSlides,
                },
            });
            if (error || !data) {
                throw new Error(describeApiError(error, IN_DEPTH_DRAFT_FALLBACK_ERROR, IN_DEPTH_FIELD_LABELS));
            }
            return data;
        },
        onSuccess: (data) => {
            setFields(data.fields);
            setAlignment(data.alignment ?? null);
            setStep("review");
        },
    });

    const redraftMutation = useMutation({
        mutationFn: async ({ slideNumber, note }: { slideNumber: number; note: string }) => {
            const { data, error } = await apiClient.POST(
                "/api/v1/category-analyses/in-depth-drafts/slide",
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
            if (error || !data) {
                throw new Error(describeApiError(error, IN_DEPTH_REDRAFT_FALLBACK_ERROR, IN_DEPTH_FIELD_LABELS));
            }
            return { data, slideNumber };
        },
        onSuccess: ({ data, slideNumber }) => {
            const merged = mergeInDepthSlideRedraft(fields, data);
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
            const { data, error } = await apiClient.POST("/api/v1/category-analyses/in-depth-decks", {
                body: {
                    category: brief.category,
                    clientName: brief.clientName,
                    selectedSlides: [...selected].sort((a, b) => a - b),
                    fields: fields.map((f) => ({ key: f.key, value: f.value })),
                },
            });
            if (error || !data) {
                throw new Error(describeApiError(error, IN_DEPTH_DECK_FALLBACK_ERROR, IN_DEPTH_FIELD_LABELS));
            }
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
    const selectedSlides = [...selected].sort((a, b) => a - b);

    function handleBriefChange(field: keyof InDepthBriefState, value: string) {
        setBrief((prev) => ({ ...prev, [field]: value }));
        setBriefErrors((prev) => ({ ...prev, [field]: undefined }));
    }

    function toggleSlide(slideNumber: number) {
        setSelected((prev) => {
            const next = new Set(prev);
            if (next.has(slideNumber)) next.delete(slideNumber);
            else next.add(slideNumber);
            return next;
        });
    }

    function handleDraftSubmit(e: React.FormEvent) {
        e.preventDefault();
        const errors: InDepthBriefFieldErrors = {};
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
        if (errors.category || errors.clientName || errors.clientWebsite || selectedSlides.length === 0) {
            return;
        }
        draftMutation.mutate({ brief, selectedSlides });
    }

    function handleFieldChange(key: string, value: string) {
        setUpdatedSlide(null);
        setFields((prev) => prev.map((f) => (f.key === key ? { ...f, value } : f)));
    }

    function handleStartOver() {
        setStep("setup");
        setBrief(EMPTY_IN_DEPTH_BRIEF);
        setBriefErrors({});
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

    return (
        <AppShell appName="Strategy Toolkit">
            <div className="cab">
                <div className="cab__nav">
                    <button
                        type="button"
                        className="cab__back"
                        onClick={() => navigate(CATEGORY_ANALYSIS_PICKER_ROUTE)}
                    >
                        ← Back
                    </button>
                    <span className="cab__breadcrumb">Strategy / Category Analysis / In-Depth</span>
                </div>

                <h1 className="cab__title">In-Depth Category Analysis</h1>
                <p className="cab__subtitle">
                    Check the slides you want from the master layout library, brief the builder, and
                    get a ready-to-edit deck in your own Google Slides with every other slide removed.
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
                        {step === "setup" && (
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
                                        <label className="cab__label" htmlFor="idab-category">
                                            Category <span className="cab__required">*</span>
                                        </label>
                                        <input
                                            id="idab-category"
                                            type="text"
                                            className="cab__input"
                                            value={brief.category}
                                            onChange={(e) => handleBriefChange("category", e.target.value)}
                                            aria-invalid={Boolean(briefErrors.category)}
                                            aria-describedby={briefErrors.category ? "idab-category-error" : undefined}
                                            placeholder="e.g. Regional Travel, Western Boots, Medicare Advantage"
                                            required
                                        />
                                        {briefErrors.category && (
                                            <p className="cab__field-error" id="idab-category-error" role="alert">
                                                {briefErrors.category}
                                            </p>
                                        )}
                                    </div>

                                    <div className="cab__field">
                                        <label className="cab__label" htmlFor="idab-client">
                                            Client <span className="cab__required">*</span>
                                        </label>
                                        <input
                                            id="idab-client"
                                            type="text"
                                            className="cab__input"
                                            value={brief.clientName}
                                            onChange={(e) => handleBriefChange("clientName", e.target.value)}
                                            aria-invalid={Boolean(briefErrors.clientName)}
                                            aria-describedby={briefErrors.clientName ? "idab-client-error" : undefined}
                                            placeholder="e.g. Alberta Boot"
                                            required
                                        />
                                        {briefErrors.clientName && (
                                            <p className="cab__field-error" id="idab-client-error" role="alert">
                                                {briefErrors.clientName}
                                            </p>
                                        )}
                                    </div>

                                    <div className="cab__field">
                                        <label className="cab__label" htmlFor="idab-website">
                                            Client website <span className="cab__required">*</span>
                                        </label>
                                        <input
                                            id="idab-website"
                                            type="url"
                                            className="cab__input"
                                            value={brief.clientWebsite}
                                            onChange={(e) => handleBriefChange("clientWebsite", e.target.value)}
                                            aria-invalid={Boolean(briefErrors.clientWebsite)}
                                            aria-describedby={briefErrors.clientWebsite ? "idab-website-error" : undefined}
                                            placeholder="e.g. https://albertaboot.com"
                                            required
                                        />
                                        {briefErrors.clientWebsite && (
                                            <p className="cab__field-error" id="idab-website-error" role="alert">
                                                {briefErrors.clientWebsite}
                                            </p>
                                        )}
                                        <p className="cab__hint">
                                            We review the site, when it can be read, to keep the deck
                                            relevant to the client's business and confirm the topic matches
                                            what they do.
                                        </p>
                                    </div>

                                    <div className="cab__field">
                                        <label className="cab__label" htmlFor="idab-notes">Focus notes</label>
                                        <textarea
                                            id="idab-notes"
                                            className="cab__textarea"
                                            value={brief.guidanceNotes}
                                            onChange={(e) => handleBriefChange("guidanceNotes", e.target.value)}
                                            placeholder="Optional angles, audiences, or constraints to steer the draft…"
                                            rows={3}
                                        />
                                    </div>
                                </div>

                                <div className="cab__divider">Check the slides to include</div>

                                {slidesQuery.isLoading && <LoadingBlock />}
                                {slidesQuery.isError && <ErrorAlert message={IN_DEPTH_CATALOG_ERROR} />}
                                {slidesQuery.data && (
                                    <div className="cab__checklist">
                                        {slidesQuery.data.map((slide: InDepthSlide) => {
                                            const checked = selected.has(slide.slideNumber);
                                            return (
                                                <label
                                                    key={slide.slideNumber}
                                                    className={`cab__checklist-card ${checked ? "cab__checklist-card--checked" : ""}`}
                                                >
                                                    <span className="cab__checklist-thumb">
                                                        <img
                                                            className="cab__checklist-image"
                                                            src={`${IN_DEPTH_SLIDE_PREVIEW_PATH}/slide-${slide.slideNumber}.jpg`}
                                                            alt={`Preview of slide ${slide.slideNumber}: ${slide.title}`}
                                                            loading="lazy"
                                                        />
                                                        <input
                                                            type="checkbox"
                                                            className="cab__checklist-checkbox"
                                                            checked={checked}
                                                            onChange={() => toggleSlide(slide.slideNumber)}
                                                        />
                                                    </span>
                                                    <span className="cab__checklist-caption">
                                                        <span className="cab__checklist-num">{slide.slideNumber}</span>
                                                        <span className="cab__checklist-title">{slide.title}</span>
                                                    </span>
                                                </label>
                                            );
                                        })}
                                    </div>
                                )}
                                {slidesQuery.data && selectedSlides.length === 0 && (
                                    <p className="cab__hint" role="alert">Check at least one slide to include.</p>
                                )}

                                {draftMutation.isError && (
                                    <ErrorAlert message={draftMutation.error.message} />
                                )}

                                <button
                                    type="submit"
                                    className="cab__submit"
                                    disabled={
                                        !aiConnected ||
                                        !brief.category.trim() ||
                                        !brief.clientName.trim() ||
                                        !brief.clientWebsite.trim() ||
                                        selectedSlides.length === 0 ||
                                        draftMutation.isPending
                                    }
                                >
                                    {draftMutation.isPending
                                        ? "Drafting content…"
                                        : `Draft ${selectedSlides.length || ""} checked slide${selectedSlides.length === 1 ? "" : "s"}`}
                                </button>
                            </form>
                        )}

                        {step === "review" && (
                            <div className="cab__form">
                                <div className="cab__review-head">
                                    <h2 className="cab__review-title">Review the draft</h2>
                                    <p className="cab__review-sub">
                                        Edit anything below. Stats and sources are AI-drafted with the
                                        help of live web search — verify them before sharing with the client.
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

                                {selectedSlides.map((slide) => {
                                    const slideFields = fields.filter((f) => f.slideNumbers.includes(slide));
                                    if (slideFields.length === 0) return null;
                                    const catalogTitle = slidesQuery.data?.find((s) => s.slideNumber === slide)?.title;
                                    return (
                                        <div
                                            className={`cab__slide-group ${
                                                updatedSlide === slide ? "cab__slide-group--updated" : ""
                                            }`}
                                            key={slide}
                                        >
                                            <div className="cab__slide-head">
                                                <h3 className="cab__slide-title">
                                                    Slide {slide}
                                                    {catalogTitle ? ` — ${baseTopic(catalogTitle)}` : ""}
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
                                            {slideFields.map((field) => (
                                                <div className="cab__field" key={field.key}>
                                                    <label className="cab__label" htmlFor={`idab-f-${field.key}`}>
                                                        {field.label}
                                                        {field.slideNumbers.length > 1 && (
                                                            <span className="cab__field-shared">
                                                                {" "}
                                                                (shared with slide
                                                                {field.slideNumbers.length > 2 ? "s" : ""}{" "}
                                                                {field.slideNumbers.filter((n) => n !== slide).join(", ")})
                                                            </span>
                                                        )}
                                                    </label>
                                                    {field.value.length > IN_DEPTH_LONG_VALUE_THRESHOLD ? (
                                                        <textarea
                                                            id={`idab-f-${field.key}`}
                                                            className="cab__textarea"
                                                            value={field.value}
                                                            rows={2}
                                                            onChange={(e) => handleFieldChange(field.key, e.target.value)}
                                                        />
                                                    ) : (
                                                        <input
                                                            id={`idab-f-${field.key}`}
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
                                                        <label className="cab__label" htmlFor={`idab-redraft-${slide}`}>
                                                            Redraft guidance (optional)
                                                        </label>
                                                        <textarea
                                                            id={`idab-redraft-${slide}`}
                                                            className="cab__textarea"
                                                            placeholder="e.g. make it more data-driven, focus on Gen Z, punchier headline…"
                                                            rows={2}
                                                            value={slideNotes[slide] ?? ""}
                                                            onChange={(e) =>
                                                                setSlideNotes((p) => ({ ...p, [slide]: e.target.value }))
                                                            }
                                                            disabled={redraftMutation.isPending && activeRedraftSlide === slide}
                                                        />
                                                        <div className="cab__redraft-actions">
                                                            <button
                                                                type="button"
                                                                className="cab__secondary"
                                                                onClick={() => setOpenNoteSlide(null)}
                                                                disabled={redraftMutation.isPending && activeRedraftSlide === slide}
                                                            >
                                                                Cancel
                                                            </button>
                                                            <button
                                                                type="button"
                                                                className="cab__redraft-go"
                                                                onClick={() => handleRedraft(slide)}
                                                                disabled={!aiConnected || redraftMutation.isPending}
                                                            >
                                                                {redraftMutation.isPending && activeRedraftSlide === slide
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
                                                    <ErrorAlert message={redraftMutation.error.message} />
                                                )}
                                            </div>
                                        </div>
                                    );
                                })}

                                <GoogleAccessNotice status={googleStatus} />

                                {deckMutation.isError && (
                                    <ErrorAlert message={deckMutation.error.message} />
                                )}

                                <div className="cab__actions">
                                    <button type="button" className="cab__secondary" onClick={handleStartOver}>
                                        Start over
                                    </button>
                                    <button
                                        type="button"
                                        className="cab__submit"
                                        disabled={!googleConnected || deckMutation.isPending || redraftMutation.isPending}
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
                                            Your Category Analysis deck is ready in your Google Drive,
                                            with only the checked slides included.
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
                                1. Check which slides of the master layout library to include.
                                <br />
                                2. AI drafts just those slides — grounded in live web search — and you
                                review and edit.
                                <br />
                                3. The deck is created from the master template in your own Google
                                Slides, with every unchecked slide removed.
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
