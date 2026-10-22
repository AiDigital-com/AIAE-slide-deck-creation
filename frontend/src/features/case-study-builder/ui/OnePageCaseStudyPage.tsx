import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import { useClerk } from "@clerk/clerk-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { AppShell } from "../../../app/AppShell";
import { describeApiError } from "../../../shared/api/apiError";
import { apiClient } from "../../../shared/api/client";
import { ErrorAlert } from "../../../shared/ui/ErrorAlert";
import { LoadingBlock } from "../../../shared/ui/LoadingBlock";
import { ACCEPTED_SOURCE_FILE_EXTENSIONS, MAX_SOURCE_FILES } from "../constants/form";
import {
    ONE_PAGE_CHARS_PER_ROW,
    ONE_PAGE_DECK_FALLBACK_ERROR,
    ONE_PAGE_DRAFT_FALLBACK_ERROR,
    ONE_PAGE_FIELD_LABELS,
    ONE_PAGE_HOUSE_PHOTO_NOTICE,
    ONE_PAGE_MEASURE_DEBOUNCE_MS,
    ONE_PAGE_SUPPORTED_FILES_HELP,
    ONE_PAGE_TEXTAREA_MAX_ROWS,
    ONE_PAGE_TEXTAREA_MIN_ROWS,
    ONE_PAGE_TEXTAREA_THRESHOLD,
} from "../constants/onePage";
import { CASE_STUDY_PICKER_ROUTE } from "../constants/templates";
import type { OnePageField, OnePageGoogleStatus, OnePageIcon, OnePageIconOption, OnePageStep } from "../model/types";
import {
    applyMeasurement,
    buildCreateRequest,
    countOverBudget,
    groupFieldsBySection,
    isMeasured,
    isOverBudget,
    lineTarget,
    lineTargetTitle,
    mergeSourceFiles,
} from "./onePageCaseStudy";
import "./one-page-case-study.css";

/**
 * Clerk-backed re-consent action — the same Clerk Google connection backs every deck tool, so
 * the fix for a missing scope is the same as on the Category Analysis pages.
 */
function GoogleReconnectAction({ status }: { status: OnePageGoogleStatus }) {
    const clerk = useClerk();
    if (status === "MISSING_SCOPES") {
        return (
            <button type="button" className="ocs__notice-action" onClick={() => clerk.openUserProfile()}>
                Reconnect Google
            </button>
        );
    }
    return (
        <button
            type="button"
            className="ocs__notice-action"
            onClick={() => clerk.signOut({ redirectUrl: "/login" })}
        >
            Sign in with Google
        </button>
    );
}

/** Targeted fix-it notice for the user's Google connection; nothing when access is granted. */
function GoogleAccessNotice({ status }: { status: OnePageGoogleStatus }) {
    if (status === "CONNECTED") return null;
    const notConnected = status === "NOT_CONNECTED";
    return (
        <div className="ocs__notice ocs__notice--warning">
            <p className="ocs__notice-title">
                {notConnected ? "You're not signed in with Google" : "Google Drive & Slides access wasn't granted"}
            </p>
            <p className="ocs__notice-body">
                {notConnected
                    ? "The deck is created in your own Google Drive. Sign in with your Google account to continue."
                    : "You're signed in with Google, but Drive & Slides access wasn't allowed. Reconnect Google and allow Drive & Slides access."}
            </p>
            <GoogleReconnectAction status={status} />
        </div>
    );
}

/** Persistent reminder that the slide's hero photo must be swapped by the user. */
function HousePhotoNotice() {
    return (
        <div className="ocs__notice">
            <p className="ocs__notice-title">Replace the photo</p>
            <p className="ocs__notice-body">{ONE_PAGE_HOUSE_PHOTO_NOTICE}</p>
        </div>
    );
}

/** Icon rendered on a brand tile (library icons are drawn for a dark background). */
function IconTile({ option, size }: { option: OnePageIconOption | null; size: "large" | "small" }) {
    return (
        <span className={`ocs__icon-tile ocs__icon-tile--size-${size}`}>
            {option ? (
                <img className="ocs__icon-img" src={option.imageUrl} alt="" />
            ) : (
                <span className="ocs__icon-placeholder">Template icon</span>
            )}
        </span>
    );
}

/** Rows a review textarea gets: the box's line count, or an estimate from the character budget. */
function textareaRows(field: OnePageField): number {
    const wanted = field.maxLines || Math.ceil(field.maxChars / ONE_PAGE_CHARS_PER_ROW);
    return Math.min(ONE_PAGE_TEXTAREA_MAX_ROWS, Math.max(ONE_PAGE_TEXTAREA_MIN_ROWS, wanted));
}

/**
 * One-Page Case Study — drafts the single-slide case study from uploaded files, lets the user
 * review every box and icon (re-measuring edits against the real slide), and creates the deck
 * in their own Google Slides.
 */
export function OnePageCaseStudyPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [step, setStep] = useState<OnePageStep>("upload");
    const [sourceFiles, setSourceFiles] = useState<File[]>([]);
    const [fileError, setFileError] = useState<string | null>(null);
    const [dragActive, setDragActive] = useState(false);
    const [fields, setFields] = useState<OnePageField[]>([]);
    const [icons, setIcons] = useState<OnePageIcon[]>([]);
    const [iconLibrary, setIconLibrary] = useState<OnePageIconOption[]>([]);
    const [sourceFileNames, setSourceFileNames] = useState("");
    const [openPickerSlot, setOpenPickerSlot] = useState<string | null>(null);
    const [slidesUrl, setSlidesUrl] = useState<string | null>(null);
    const [elapsedSeconds, setElapsedSeconds] = useState(0);
    const measureTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
    const fieldsRef = useRef<OnePageField[]>([]);
    fieldsRef.current = fields;
    const pickerRef = useRef<HTMLDivElement | null>(null);

    const connectionsQuery = useQuery({
        queryKey: ["category-analyses", "standard-connections"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/category-analyses/standard-connections");
            if (error) throw new Error("Failed to check connections");
            return data;
        },
    });

    const draftMutation = useMutation({
        mutationFn: async (files: File[]) => {
            const formData = new FormData();
            files.forEach((file) => formData.append("files", file));
            const { data, error } = await apiClient.POST("/api/v1/case-studies/one-page-drafts", {
                body: formData as unknown as { files: string[] },
                bodySerializer: () => formData,
            });
            if (error || !data) {
                throw new Error(describeApiError(error, ONE_PAGE_DRAFT_FALLBACK_ERROR, ONE_PAGE_FIELD_LABELS));
            }
            return data;
        },
        onSuccess: (draft) => {
            setFields(draft.fields);
            setIcons(draft.icons);
            setIconLibrary(draft.iconLibrary);
            setSourceFileNames(draft.sourceFileNames);
            setStep("review");
            window.scrollTo({ top: 0, behavior: "smooth" });
        },
    });

    const measureMutation = useMutation({
        mutationFn: async (snapshot: OnePageField[]) => {
            const { data, error } = await apiClient.POST("/api/v1/case-studies/one-page-fit", {
                body: { fields: snapshot.map((f) => ({ key: f.key, value: f.value })) },
            });
            if (error || !data) throw new Error("Could not measure the edited fields");
            return { snapshot, measured: data };
        },
        onSuccess: ({ snapshot, measured }) => {
            setFields((prev) => applyMeasurement(prev, snapshot, measured));
        },
    });

    const deckMutation = useMutation({
        mutationFn: async () => {
            const { data, error } = await apiClient.POST("/api/v1/case-studies/one-page-decks", {
                body: buildCreateRequest(fields, icons, sourceFileNames),
            });
            if (error || !data) {
                throw new Error(describeApiError(error, ONE_PAGE_DECK_FALLBACK_ERROR, ONE_PAGE_FIELD_LABELS));
            }
            return data;
        },
        onSuccess: (data) => {
            setSlidesUrl(data.slidesUrl ?? null);
            setStep("done");
            queryClient.invalidateQueries({ queryKey: ["case-studies"] });
            window.scrollTo({ top: 0, behavior: "smooth" });
        },
    });

    // Drafting takes ~30–60s; show elapsed time so the wait reads as progress, not a hang.
    useEffect(() => {
        if (!draftMutation.isPending) return;
        setElapsedSeconds(0);
        const timer = window.setInterval(() => setElapsedSeconds((s) => s + 1), 1000);
        return () => window.clearInterval(timer);
    }, [draftMutation.isPending]);

    // Close the icon picker on outside click or Escape.
    useEffect(() => {
        if (!openPickerSlot) return;
        function handlePointer(e: MouseEvent) {
            if (pickerRef.current && !pickerRef.current.contains(e.target as Node)) setOpenPickerSlot(null);
        }
        function handleKey(e: KeyboardEvent) {
            if (e.key === "Escape") setOpenPickerSlot(null);
        }
        document.addEventListener("mousedown", handlePointer);
        document.addEventListener("keydown", handleKey);
        return () => {
            document.removeEventListener("mousedown", handlePointer);
            document.removeEventListener("keydown", handleKey);
        };
    }, [openPickerSlot]);

    // Never leave a pending re-measure behind when the page unmounts.
    useEffect(() => {
        return () => {
            if (measureTimer.current) clearTimeout(measureTimer.current);
        };
    }, []);

    const googleStatus: OnePageGoogleStatus = connectionsQuery.data?.googleStatus ?? "NOT_CONNECTED";
    const googleConnected = googleStatus === "CONNECTED";
    const aiConnected = connectionsQuery.data?.aiConnected ?? false;
    const sections = useMemo(() => groupFieldsBySection(fields), [fields]);
    const libraryByName = useMemo(() => new Map(iconLibrary.map((o) => [o.name, o])), [iconLibrary]);
    const overBudget = countOverBudget(fields);

    function addFiles(chosen: File[]) {
        if (chosen.length === 0) return;
        const result = mergeSourceFiles(sourceFiles, chosen);
        setSourceFiles(result.files);
        const problems: string[] = [];
        if (result.rejected.length > 0) {
            problems.push(`Unsupported file type: ${result.rejected.join(", ")}.`);
        }
        if (result.overflow > 0) {
            problems.push(`You can upload up to ${MAX_SOURCE_FILES} files. Extra files were not added.`);
        }
        setFileError(problems.length > 0 ? problems.join(" ") : null);
    }

    function handleFilesChosen(e: React.ChangeEvent<HTMLInputElement>) {
        const chosen = Array.from(e.target.files ?? []);
        e.target.value = "";
        addFiles(chosen);
    }

    function handleDragOver(e: React.DragEvent<HTMLDivElement>) {
        e.preventDefault();
        if (!draftMutation.isPending) setDragActive(true);
    }

    function handleDragLeave(e: React.DragEvent<HTMLDivElement>) {
        e.preventDefault();
        setDragActive(false);
    }

    function handleDrop(e: React.DragEvent<HTMLDivElement>) {
        e.preventDefault();
        setDragActive(false);
        if (draftMutation.isPending) return;
        addFiles(Array.from(e.dataTransfer.files ?? []));
    }

    function removeFile(index: number) {
        setFileError(null);
        setSourceFiles((prev) => prev.filter((_, i) => i !== index));
    }

    function handleDraftSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (draftMutation.isPending) return;
        if (sourceFiles.length === 0) {
            setFileError("Add at least one campaign file to draft from.");
            return;
        }
        setFileError(null);
        draftMutation.mutate(sourceFiles);
    }

    /**
     * Re-measures every field against its box on the slide shortly after the user stops typing,
     * so the line counts always reflect the edited text.
     */
    function scheduleMeasure() {
        if (measureTimer.current) clearTimeout(measureTimer.current);
        measureTimer.current = setTimeout(() => {
            measureMutation.mutate(fieldsRef.current);
        }, ONE_PAGE_MEASURE_DEBOUNCE_MS);
    }

    function handleFieldChange(key: string, value: string) {
        setFields((prev) => prev.map((f) => (f.key === key ? { ...f, value } : f)));
        scheduleMeasure();
    }

    function handleIconChoice(slot: string, icon: string | null) {
        setIcons((prev) => prev.map((i) => (i.slot === slot ? { ...i, icon } : i)));
        setOpenPickerSlot(null);
    }

    function handleStartOver() {
        setStep("upload");
        setSourceFiles([]);
        setFileError(null);
        setFields([]);
        setIcons([]);
        setIconLibrary([]);
        setSourceFileNames("");
        setOpenPickerSlot(null);
        setSlidesUrl(null);
        draftMutation.reset();
        deckMutation.reset();
        measureMutation.reset();
    }

    return (
        <AppShell appName="Strategy Toolkit">
            <div className="ocs">
                <div className="ocs__nav">
                    <button type="button" className="ocs__back" onClick={() => navigate(CASE_STUDY_PICKER_ROUTE)}>
                        ← Back
                    </button>
                    <span className="ocs__breadcrumb">Strategy / Case Study / One-Page</span>
                </div>

                <h1 className="ocs__title">One-Page Case Study</h1>
                <p className="ocs__subtitle">
                    Upload campaign reports, review the drafted copy and icons, and get a single-slide case study
                    in your own Google Slides.
                </p>

                {connectionsQuery.isSuccess && (
                    <div className="ocs__connections">
                        <span className={`ocs__conn ${aiConnected ? "ocs__conn--ok" : "ocs__conn--off"}`}>
                            {aiConnected ? "✓ AI drafting connected" : "✕ AI drafting not connected"}
                        </span>
                        <span className={`ocs__conn ${googleConnected ? "ocs__conn--ok" : "ocs__conn--off"}`}>
                            {googleConnected && "✓ Google Slides connected"}
                            {googleStatus === "MISSING_SCOPES" && "✕ Google Drive & Slides access not granted"}
                            {googleStatus === "NOT_CONNECTED" && "✕ Not signed in with Google"}
                        </span>
                    </div>
                )}

                <div className="ocs__layout">
                    <div className="ocs__main">
                        {step === "upload" && (
                            <form className="ocs__card" onSubmit={handleDraftSubmit}>
                                <div
                                    className={`ocs__dropzone${dragActive ? " ocs__dropzone--active" : ""}`}
                                    onDragOver={handleDragOver}
                                    onDragEnter={handleDragOver}
                                    onDragLeave={handleDragLeave}
                                    onDrop={handleDrop}
                                >
                                    <span className="ocs__dropzone-icon" aria-hidden="true">⬆</span>
                                    <span className="ocs__dropzone-title">Drag campaign files here</span>
                                    <span className="ocs__dropzone-sub" id="ocs-files-help">
                                        {ONE_PAGE_SUPPORTED_FILES_HELP}
                                    </span>
                                    <label
                                        className={`ocs__upload-btn${draftMutation.isPending ? " ocs__upload-btn--disabled" : ""}`}
                                    >
                                        Choose files
                                        <input
                                            type="file"
                                            multiple
                                            accept={ACCEPTED_SOURCE_FILE_EXTENSIONS}
                                            className="ocs__file-input"
                                            disabled={draftMutation.isPending}
                                            aria-required="true"
                                            aria-invalid={Boolean(fileError)}
                                            aria-describedby={fileError ? "ocs-files-error" : "ocs-files-help"}
                                            onChange={handleFilesChosen}
                                        />
                                    </label>
                                </div>

                                {sourceFiles.length > 0 && (
                                    <ul className="ocs__file-list">
                                        {sourceFiles.map((file, index) => (
                                            <li key={`${file.name}-${file.size}`} className="ocs__file-item">
                                                <span className="ocs__file-name">{file.name}</span>
                                                <button
                                                    type="button"
                                                    className="ocs__file-remove"
                                                    aria-label={`Remove ${file.name}`}
                                                    disabled={draftMutation.isPending}
                                                    onClick={() => removeFile(index)}
                                                >
                                                    ✕
                                                </button>
                                            </li>
                                        ))}
                                    </ul>
                                )}

                                <p className="ocs__hint">
                                    Facts come only from your files. Titles and labels may be written by AI for flow.
                                </p>

                                {fileError && (
                                    <p className="ocs__field-error" id="ocs-files-error" role="alert">
                                        {fileError}
                                    </p>
                                )}

                                {connectionsQuery.isSuccess && !aiConnected && (
                                    <div className="ocs__notice ocs__notice--warning">
                                        <p className="ocs__notice-title">AI drafting is not connected</p>
                                        <p className="ocs__notice-body">
                                            Ask your admin to add the OpenAI API key so the builder can draft the
                                            slide for you.
                                        </p>
                                    </div>
                                )}

                                {draftMutation.isPending && (
                                    <div className="ocs__progress" role="status" aria-live="polite">
                                        <span className="ocs__spinner" aria-hidden="true" />
                                        <div>
                                            <p className="ocs__progress-title">
                                                Reading {sourceFiles.length} file{sourceFiles.length > 1 ? "s" : ""} and
                                                drafting your case study… {elapsedSeconds}s
                                            </p>
                                            <p className="ocs__progress-body">
                                                This usually takes 30–60 seconds. Keep this page open.
                                            </p>
                                        </div>
                                    </div>
                                )}

                                {draftMutation.isError && <ErrorAlert message={draftMutation.error.message} />}

                                <div className="ocs__actions">
                                    <button
                                        type="submit"
                                        className="ocs__submit"
                                        disabled={sourceFiles.length === 0 || draftMutation.isPending}
                                    >
                                        {draftMutation.isPending ? "Drafting…" : "Draft case study"}
                                    </button>
                                </div>
                            </form>
                        )}

                        {step === "review" && (
                            <div className="ocs__card">
                                <div className="ocs__review-head">
                                    <h2 className="ocs__review-title">Review the draft</h2>
                                    <p className="ocs__review-sub">
                                        Edit anything before creating the deck. Drafted from: {sourceFileNames || "your files"}
                                    </p>
                                </div>

                                {sections.map((group) => (
                                    <section key={group.section} className="ocs__section">
                                        <h3 className="ocs__section-title">{group.section}</h3>
                                        <div className="ocs__fields">
                                            {group.fields.map((field) => {
                                                const id = `ocs-${field.key}`;
                                                const measured = isMeasured(field);
                                                const over = isOverBudget(field);
                                                const short = field.fit === "SHORT";
                                                const multiline = field.maxLines > 1 || field.maxChars > ONE_PAGE_TEXTAREA_THRESHOLD;
                                                const controlClass = `${multiline ? "ocs__textarea" : "ocs__input"}${over ? " ocs__input--over" : ""}`;
                                                const hintId = over || short ? `${id}-hint` : undefined;
                                                return (
                                                    <div key={field.key} className="ocs__field">
                                                        <div className="ocs__field-head">
                                                            <label className="ocs__label" htmlFor={id}>
                                                                {field.label}
                                                            </label>
                                                            <span
                                                                className={`ocs__chip ${field.source === "FILES" ? "ocs__chip--files" : "ocs__chip--written"}`}
                                                            >
                                                                {field.source === "FILES" ? "From your files" : "AI wording"}
                                                            </span>
                                                            <span
                                                                className={`ocs__counter${over ? " ocs__counter--over" : ""}${short ? " ocs__counter--short" : ""}`}
                                                                aria-live="polite"
                                                                title={lineTargetTitle(field)}
                                                            >
                                                                {measured
                                                                    ? `${field.lines} / ${lineTarget(field)} lines`
                                                                    : `${field.value.length} / ${field.maxChars}`}
                                                            </span>
                                                        </div>
                                                        {multiline ? (
                                                            <textarea
                                                                id={id}
                                                                className={controlClass}
                                                                rows={textareaRows(field)}
                                                                value={field.value}
                                                                aria-invalid={over}
                                                                aria-describedby={hintId}
                                                                onChange={(e) => handleFieldChange(field.key, e.target.value)}
                                                            />
                                                        ) : (
                                                            <input
                                                                id={id}
                                                                type="text"
                                                                className={controlClass}
                                                                value={field.value}
                                                                aria-invalid={over}
                                                                aria-describedby={hintId}
                                                                onChange={(e) => handleFieldChange(field.key, e.target.value)}
                                                            />
                                                        )}
                                                        {short && (
                                                            <span className="ocs__short-hint" id={hintId}>
                                                                {field.note ??
                                                                    `Fills ${field.lines} of ${field.minLines} lines — add detail so the box isn't partly empty.`}
                                                            </span>
                                                        )}
                                                        {over && (
                                                            <span className="ocs__over-hint" id={hintId} role="alert">
                                                                {measured
                                                                    ? `Runs past its box — shorten it to ${field.maxLines} line${field.maxLines === 1 ? "" : "s"}.`
                                                                    : `Over the ${field.maxChars}-character budget — shorten it.`}
                                                            </span>
                                                        )}
                                                        {!field.value.trim() && (
                                                            <span className="ocs__empty-hint">
                                                                Not found in your files — box will be left blank
                                                            </span>
                                                        )}
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    </section>
                                ))}

                                {icons.length > 0 && (
                                    <section className="ocs__section">
                                        <h3 className="ocs__section-title">Icons</h3>
                                        <div className="ocs__icon-slots">
                                            {icons.map((slot) => {
                                                const chosen = slot.icon ? libraryByName.get(slot.icon) ?? null : null;
                                                const pickerOpen = openPickerSlot === slot.slot;
                                                return (
                                                    <div
                                                        key={slot.slot}
                                                        ref={pickerOpen ? pickerRef : undefined}
                                                        className="ocs__icon-slot"
                                                    >
                                                        <IconTile option={chosen} size="large" />
                                                        <div className="ocs__icon-meta">
                                                            <span className="ocs__icon-label">{slot.label}</span>
                                                            <span className="ocs__icon-name">
                                                                {chosen ? chosen.description : "Template icon"}
                                                            </span>
                                                            <button
                                                                type="button"
                                                                className="ocs__icon-change"
                                                                aria-expanded={pickerOpen}
                                                                aria-haspopup="true"
                                                                onClick={() => setOpenPickerSlot(pickerOpen ? null : slot.slot)}
                                                            >
                                                                Change icon
                                                            </button>
                                                        </div>
                                                        {pickerOpen && (
                                                            <div
                                                                className="ocs__picker"
                                                                role="group"
                                                                aria-label={`Choose icon for ${slot.label}`}
                                                            >
                                                                <button
                                                                    type="button"
                                                                    className={`ocs__picker-keep${slot.icon ? "" : " ocs__picker-keep--selected"}`}
                                                                    aria-pressed={!slot.icon}
                                                                    onClick={() => handleIconChoice(slot.slot, null)}
                                                                >
                                                                    Keep template icon
                                                                </button>
                                                                <div className="ocs__picker-grid">
                                                                    {iconLibrary.map((option) => {
                                                                        const selected = slot.icon === option.name;
                                                                        return (
                                                                            <button
                                                                                key={option.name}
                                                                                type="button"
                                                                                className={`ocs__picker-option${selected ? " ocs__picker-option--selected" : ""}`}
                                                                                title={option.description}
                                                                                aria-label={option.description}
                                                                                aria-pressed={selected}
                                                                                onClick={() => handleIconChoice(slot.slot, option.name)}
                                                                            >
                                                                                <IconTile option={option} size="small" />
                                                                            </button>
                                                                        );
                                                                    })}
                                                                </div>
                                                            </div>
                                                        )}
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    </section>
                                )}

                                {overBudget > 0 && (
                                    <p className="ocs__over-note" role="status">
                                        {overBudget} field{overBudget > 1 ? "s" : ""} may overflow{" "}
                                        {overBudget > 1 ? "their boxes" : "its box"} on the slide — shorten{" "}
                                        {overBudget > 1 ? "them" : "it"} before creating the deck.
                                    </p>
                                )}

                                <HousePhotoNotice />

                                {connectionsQuery.isSuccess && <GoogleAccessNotice status={googleStatus} />}

                                {deckMutation.isPending && <LoadingBlock label="Creating your deck in Google Slides…" />}

                                {deckMutation.isError && <ErrorAlert message={deckMutation.error.message} />}

                                <div className="ocs__actions">
                                    <button
                                        type="button"
                                        className="ocs__secondary"
                                        disabled={deckMutation.isPending}
                                        onClick={handleStartOver}
                                    >
                                        Start over
                                    </button>
                                    <button
                                        type="button"
                                        className="ocs__submit"
                                        disabled={deckMutation.isPending || (connectionsQuery.isSuccess && !googleConnected)}
                                        onClick={() => deckMutation.mutate()}
                                    >
                                        {deckMutation.isPending ? "Creating deck…" : "Create deck in Google Slides"}
                                    </button>
                                </div>
                            </div>
                        )}

                        {step === "done" && (
                            <div className="ocs__card">
                                <div className="ocs__success">
                                    <span className="ocs__success-icon" aria-hidden="true">✓</span>
                                    <div className="ocs__success-text">
                                        <p className="ocs__success-title">Deck created!</p>
                                        <p className="ocs__success-body">
                                            Your one-page case study is ready in your Google Drive.
                                        </p>
                                    </div>
                                </div>
                                <HousePhotoNotice />
                                <div className="ocs__actions">
                                    <button type="button" className="ocs__secondary" onClick={handleStartOver}>
                                        Create another
                                    </button>
                                    {slidesUrl && (
                                        <a
                                            className="ocs__slides-link"
                                            href={slidesUrl}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                        >
                                            Open in Google Slides →
                                        </a>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>

                    <aside className="ocs__aside">
                        <div className="ocs__aside-card">
                            <h2 className="ocs__aside-title">How it works</h2>
                            <ol className="ocs__steps">
                                <li className={`ocs__step${step === "upload" ? " ocs__step--active" : ""}`}>
                                    Upload campaign reports, decks, or screenshots.
                                </li>
                                <li className={`ocs__step${step === "review" ? " ocs__step--active" : ""}`}>
                                    AI fills every box from your files — you review copy and icons.
                                </li>
                                <li className={`ocs__step${step === "done" ? " ocs__step--active" : ""}`}>
                                    The slide is created in your own Google Slides. Swap in a client photo.
                                </li>
                            </ol>
                        </div>
                    </aside>
                </div>
            </div>
        </AppShell>
    );
}
