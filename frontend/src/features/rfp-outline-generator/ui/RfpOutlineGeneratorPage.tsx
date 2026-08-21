import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { AppShell } from "../../../app/AppShell";
import { apiClient } from "../../../shared/api/client";
import { ErrorAlert } from "../../../shared/ui/ErrorAlert";
import { LoadingBlock } from "../../../shared/ui/LoadingBlock";
import { EMPTY_FORM, MAX_SOURCE_FILES } from "../constants/form";
import type { FieldErrors, FormState } from "../model/types";
import "./rfp-outline-generator.css";

/** RFP Outline Generator page. */
export function RfpOutlineGeneratorPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const [submitted, setSubmitted] = useState<{ docUrl: string | null } | null>(null);
    const [sourceFiles, setSourceFiles] = useState<File[]>([]);
    const [fileError, setFileError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
    const [dragActive, setDragActive] = useState(false);

    const recentQuery = useQuery({
        queryKey: ["rfp-outlines"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/rfp-outlines");
            if (error) throw new Error("Failed to load recent RFP outlines");
            return data;
        },
    });

    const createMutation = useMutation({
        mutationFn: async (formData: FormState) => {
            const { data, error } = await apiClient.POST("/api/v1/rfp-outlines", {
                body: {
                    title: formData.title,
                    clientName: formData.clientName || null,
                    industry: formData.industry || null,
                    challenge: formData.challenge || null,
                    opportunity: formData.opportunity || null,
                    solution: formData.solution || null,
                    outcome: formData.outcome || null,
                    deckOutline: formData.deckOutline || null,
                    supplementaryNotes: formData.supplementaryNotes || null,
                    sourceFileName: formData.sourceFileName || null,
                },
            });
            if (error) {
                const detail = (error as { message?: string } | null)?.message;
                throw new Error(detail || "Failed to create RFP outline");
            }
            return data;
        },
        onSuccess: (data) => {
            queryClient.invalidateQueries({ queryKey: ["rfp-outlines"] });
            setSubmitted({ docUrl: data?.docUrl ?? null });
            setForm(EMPTY_FORM);
        },
    });

    const draftMutation = useMutation({
        mutationFn: async ({ files, notes }: { files: File[]; notes: string }) => {
            const formData = new FormData();
            files.forEach((file) => formData.append("files", file));
            if (notes.trim()) formData.append("notes", notes);
            const { data, error } = await apiClient.POST("/api/v1/rfp-outlines/drafts", {
                body: formData as unknown as { files: string[] },
                bodySerializer: () => formData,
            });
            if (error) throw new Error("Failed to draft RFP outline from files");
            return data;
        },
        onSuccess: (draft) => {
            if (!draft) return;
            setForm((prev) => ({
                title: draft.title ?? prev.title,
                clientName: draft.clientName ?? prev.clientName,
                industry: draft.industry ?? prev.industry,
                challenge: draft.challenge ?? prev.challenge,
                opportunity: draft.opportunity ?? prev.opportunity,
                solution: draft.solution ?? prev.solution,
                outcome: draft.outcome ?? prev.outcome,
                deckOutline: draft.deckOutline ?? prev.deckOutline,
                supplementaryNotes: prev.supplementaryNotes,
                sourceFileName: draft.sourceFileNames,
            }));
        },
    });

    function handleChange(field: keyof FormState, value: string) {
        setForm((prev) => ({ ...prev, [field]: value }));
        setFieldErrors((prev) => ({ ...prev, [field]: undefined }));
    }

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!form.title.trim()) {
            setFieldErrors({ title: "Enter a title before generating the outline." });
            return;
        }
        setFieldErrors({});
        setSubmitted(null);
        createMutation.mutate(form);
    }

    function addFiles(chosen: File[]) {
        if (chosen.length === 0) return;
        setFileError(null);
        setSourceFiles((prev) => {
            const merged = [...prev];
            for (const file of chosen) {
                if (!merged.some((f) => f.name === file.name && f.size === file.size)) {
                    merged.push(file);
                }
            }
            if (merged.length > MAX_SOURCE_FILES) {
                setFileError(`You can upload up to ${MAX_SOURCE_FILES} files. Extra files were not added.`);
                return merged.slice(0, MAX_SOURCE_FILES);
            }
            return merged;
        });
    }

    function handleFilesChosen(e: React.ChangeEvent<HTMLInputElement>) {
        const chosen = Array.from(e.target.files ?? []);
        e.target.value = "";
        addFiles(chosen);
    }

    function handleDragOver(e: React.DragEvent<HTMLDivElement>) {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(true);
    }

    function handleDragLeave(e: React.DragEvent<HTMLDivElement>) {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);
    }

    function handleDrop(e: React.DragEvent<HTMLDivElement>) {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);
        addFiles(Array.from(e.dataTransfer.files ?? []));
    }

    function removeFile(index: number) {
        setFileError(null);
        setSourceFiles((prev) => prev.filter((_, i) => i !== index));
    }

    function handleAutoDraft() {
        if (sourceFiles.length === 0) return;
        setFileError(null);
        draftMutation.mutate({ files: sourceFiles, notes: form.supplementaryNotes });
    }

    return (
        <AppShell appName="Strategy & Planning">
            <div className="rog">
                <div className="rog__nav">
                    <button type="button" className="rog__back" onClick={() => navigate("/")}>
                        ← Back
                    </button>
                    <span className="rog__breadcrumb">Strategy / Documents</span>
                </div>

                <h1 className="rog__title">RFP Outline Generator</h1>
                <p className="rog__subtitle">
                    Upload an RFP and draft a succinct POV plus a response deck outline.
                </p>

                <div className="rog__layout">
                    <div className="rog__main">
                        <form className="rog__form" onSubmit={handleSubmit}>
                            <div
                                className={`rog__auto-draft rog__dropzone${dragActive ? " rog__dropzone--active" : ""}`}
                                onDragOver={handleDragOver}
                                onDragEnter={handleDragOver}
                                onDragLeave={handleDragLeave}
                                onDrop={handleDrop}
                            >
                                <div className="rog__auto-draft-label">
                                    <span className="rog__auto-draft-icon" aria-hidden="true">⬆</span>
                                    <div>
                                        <span className="rog__auto-draft-title">Auto-Draft</span>
                                        <span className="rog__auto-draft-sub">
                                            Drag files here, or upload up to {MAX_SOURCE_FILES} (PDF, Word, PowerPoint, Excel, CSV)
                                        </span>
                                    </div>
                                </div>
                                <label className="rog__upload-btn">
                                    Add Files
                                    <input
                                        type="file"
                                        multiple
                                        accept=".pdf,.docx,.doc,.pptx,.ppt,.xlsx,.xls,.csv"
                                        className="rog__file-input"
                                        onChange={handleFilesChosen}
                                    />
                                </label>
                            </div>

                            {sourceFiles.length > 0 && (
                                <ul className="rog__file-list">
                                    {sourceFiles.map((file, index) => (
                                        <li key={`${file.name}-${file.size}`} className="rog__file-item">
                                            <span className="rog__file-name">{file.name}</span>
                                            <button
                                                type="button"
                                                className="rog__file-remove"
                                                aria-label={`Remove ${file.name}`}
                                                onClick={() => removeFile(index)}
                                            >
                                                ✕
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}

                            <div className="rog__field">
                                <label className="rog__label" htmlFor="rog-notes">
                                    Supplementary notes (optional)
                                </label>
                                <textarea
                                    id="rog-notes"
                                    className="rog__textarea"
                                    value={form.supplementaryNotes}
                                    onChange={(e) => handleChange("supplementaryNotes", e.target.value)}
                                    placeholder="Any extra context not captured in the uploaded RFP..."
                                    rows={2}
                                />
                            </div>

                            {fileError && <ErrorAlert message={fileError} />}

                            {sourceFiles.length > 0 && (
                                <div className="rog__actions rog__actions--draft">
                                    <button
                                        type="button"
                                        className="rog__submit"
                                        disabled={draftMutation.isPending}
                                        onClick={handleAutoDraft}
                                    >
                                        {draftMutation.isPending
                                            ? "Drafting from files…"
                                            : `Auto-Draft from ${sourceFiles.length} file${sourceFiles.length > 1 ? "s" : ""}`}
                                    </button>
                                </div>
                            )}

                            {draftMutation.isError && (
                                <ErrorAlert message="Could not draft from the uploaded files. Check the file types and try again." />
                            )}

                            {draftMutation.isSuccess && (
                                <p className="rog__file-chosen">
                                    Draft ready — review and edit the fields below, then export to a Google Doc.
                                </p>
                            )}

                            <div className="rog__divider">
                                <span className="rog__divider-text">OR FILL DETAILS</span>
                            </div>

                            <div className="rog__fields">
                                <div className="rog__field">
                                    <label className="rog__label" htmlFor="rog-title">Title <span className="rog__required">*</span></label>
                                    <input
                                        id="rog-title"
                                        type="text"
                                        className="rog__input"
                                        value={form.title}
                                        onChange={(e) => handleChange("title", e.target.value)}
                                        placeholder="e.g. Acme Corp — Media RFP Response Outline"
                                        required
                                        aria-invalid={Boolean(fieldErrors.title)}
                                        aria-describedby={fieldErrors.title ? "rog-title-error" : undefined}
                                    />
                                    {fieldErrors.title && (
                                        <p className="rog__field-error" id="rog-title-error" role="alert">
                                            {fieldErrors.title}
                                        </p>
                                    )}
                                </div>

                                <div className="rog__field-row">
                                    <div className="rog__field">
                                        <label className="rog__label" htmlFor="rog-client">Client Name</label>
                                        <input
                                            id="rog-client"
                                            type="text"
                                            className="rog__input"
                                            value={form.clientName}
                                            onChange={(e) => handleChange("clientName", e.target.value)}
                                            placeholder="e.g. Acme Corp"
                                        />
                                    </div>
                                    <div className="rog__field">
                                        <label className="rog__label" htmlFor="rog-industry">Industry</label>
                                        <input
                                            id="rog-industry"
                                            type="text"
                                            className="rog__input"
                                            value={form.industry}
                                            onChange={(e) => handleChange("industry", e.target.value)}
                                            placeholder="e.g. Higher Education"
                                        />
                                    </div>
                                </div>

                                <div className="rog__field">
                                    <label className="rog__label" htmlFor="rog-challenge">Challenge</label>
                                    <textarea
                                        id="rog-challenge"
                                        className="rog__textarea"
                                        value={form.challenge}
                                        onChange={(e) => handleChange("challenge", e.target.value)}
                                        placeholder="The client's core challenge or need..."
                                        rows={2}
                                    />
                                </div>

                                <div className="rog__field">
                                    <label className="rog__label" htmlFor="rog-opportunity">Opportunity</label>
                                    <textarea
                                        id="rog-opportunity"
                                        className="rog__textarea"
                                        value={form.opportunity}
                                        onChange={(e) => handleChange("opportunity", e.target.value)}
                                        placeholder="The differentiator or angle for this client..."
                                        rows={2}
                                    />
                                </div>

                                <div className="rog__field">
                                    <label className="rog__label" htmlFor="rog-solution">Solution</label>
                                    <textarea
                                        id="rog-solution"
                                        className="rog__textarea"
                                        value={form.solution}
                                        onChange={(e) => handleChange("solution", e.target.value)}
                                        placeholder="The proposed strategic approach..."
                                        rows={2}
                                    />
                                </div>

                                <div className="rog__field">
                                    <label className="rog__label" htmlFor="rog-outcome">Outcome</label>
                                    <textarea
                                        id="rog-outcome"
                                        className="rog__textarea"
                                        value={form.outcome}
                                        onChange={(e) => handleChange("outcome", e.target.value)}
                                        placeholder="What success looks like..."
                                        rows={2}
                                    />
                                </div>

                                <div className="rog__field">
                                    <label className="rog__label" htmlFor="rog-outline">Response Deck Outline</label>
                                    <textarea
                                        id="rog-outline"
                                        className="rog__textarea"
                                        value={form.deckOutline}
                                        onChange={(e) => handleChange("deckOutline", e.target.value)}
                                        placeholder="Slide-by-slide breakdown of how the response deck should be structured..."
                                        rows={24}
                                    />
                                </div>
                            </div>

                            {createMutation.isError && (
                                <ErrorAlert
                                    message={
                                        createMutation.error instanceof Error && createMutation.error.message
                                            ? createMutation.error.message
                                            : "Failed to export the RFP outline. Please try again."
                                    }
                                />
                            )}

                            {submitted && (
                                <div className="rog__success">
                                    <span className="rog__success-icon" aria-hidden="true">✓</span>
                                    <div>
                                        <p className="rog__success-title">RFP Outline saved!</p>
                                        <p className="rog__success-body">
                                            {submitted.docUrl ? (
                                                <>
                                                    Your RFP outline is ready in your Google Drive.{" "}
                                                    <a
                                                        href={submitted.docUrl}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="rog__template-link"
                                                    >
                                                        Open in Google Doc
                                                    </a>
                                                    .
                                                </>
                                            ) : (
                                                "Your RFP outline has been saved."
                                            )}
                                        </p>
                                    </div>
                                </div>
                            )}

                            <div className="rog__actions">
                                <button
                                    type="submit"
                                    className="rog__submit"
                                    disabled={!form.title.trim() || createMutation.isPending}
                                >
                                    {createMutation.isPending ? "Exporting…" : "Save & Export to Google Doc"}
                                </button>
                            </div>
                        </form>
                    </div>

                    <aside className="rog__aside">
                        <div className="rog__recent">
                            <h2 className="rog__recent-title">Recent RFP Outlines</h2>
                            {recentQuery.isLoading && <LoadingBlock />}
                            {recentQuery.isError && <p className="rog__recent-empty">Could not load recent RFP outlines.</p>}
                            {recentQuery.data?.length === 0 && (
                                <p className="rog__recent-empty">No recent RFP outlines.</p>
                            )}
                            {recentQuery.data && recentQuery.data.length > 0 && (
                                <ul className="rog__recent-list">
                                    {recentQuery.data.map((outline) => (
                                        <li key={outline.id} className="rog__recent-item">
                                            <span className="rog__recent-name">{outline.title}</span>
                                            {outline.clientName && (
                                                <span className="rog__recent-meta">{outline.clientName}</span>
                                            )}
                                            <span className={`rog__recent-badge rog__recent-badge--${outline.status?.toLowerCase()}`}>
                                                {outline.status === "GENERATED"
                                                    ? "Generated"
                                                    : outline.status === "SUBMITTED"
                                                        ? "Submitted"
                                                        : "Draft"}
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
