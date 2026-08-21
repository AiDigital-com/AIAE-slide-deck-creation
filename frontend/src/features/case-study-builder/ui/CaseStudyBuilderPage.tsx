import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { AppShell } from "../../../app/AppShell";
import { apiClient } from "../../../shared/api/client";
import { ErrorAlert } from "../../../shared/ui/ErrorAlert";
import { LoadingBlock } from "../../../shared/ui/LoadingBlock";
import { EMPTY_FORM, MAX_SOURCE_FILES } from "../constants/form";
import type { FieldErrors, FormState } from "../model/types";
import "./case-study-builder.css";

/** Case Study Builder page. */
export function CaseStudyBuilderPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const [submitted, setSubmitted] = useState<{ slidesUrl: string | null } | null>(null);
    const [sourceFiles, setSourceFiles] = useState<File[]>([]);
    const [fileError, setFileError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

    const recentQuery = useQuery({
        queryKey: ["case-studies"],
        queryFn: async () => {
            const { data, error } = await apiClient.GET("/api/v1/case-studies");
            if (error) throw new Error("Failed to load recent studies");
            return data;
        },
    });

    const createMutation = useMutation({
        mutationFn: async (formData: FormState) => {
            const { data, error } = await apiClient.POST("/api/v1/case-studies", {
                body: {
                    title: formData.title,
                    clientName: formData.clientName || null,
                    industry: formData.industry || null,
                    challenge: formData.challenge || null,
                    solution: formData.solution || null,
                    results: formData.results || null,
                    keyMetrics: formData.keyMetrics || null,
                    timeline: formData.timeline || null,
                    testimonial: formData.testimonial || null,
                    sourceFileName: formData.sourceFileName || null,
                },
            });
            if (error) {
                const detail = (error as { message?: string } | null)?.message;
                throw new Error(detail || "Failed to create case study");
            }
            return data;
        },
        onSuccess: (data) => {
            queryClient.invalidateQueries({ queryKey: ["case-studies"] });
            setSubmitted({ slidesUrl: data?.slidesUrl ?? null });
            setForm(EMPTY_FORM);
        },
    });

    const draftMutation = useMutation({
        mutationFn: async (files: File[]) => {
            const formData = new FormData();
            files.forEach((file) => formData.append("files", file));
            const { data, error } = await apiClient.POST("/api/v1/case-studies/drafts", {
                body: formData as unknown as { files: string[] },
                bodySerializer: () => formData,
            });
            if (error) throw new Error("Failed to draft case study from files");
            return data;
        },
        onSuccess: (draft) => {
            if (!draft) return;
            setForm((prev) => ({
                title: draft.title ?? prev.title,
                clientName: draft.clientName ?? prev.clientName,
                industry: draft.industry ?? prev.industry,
                challenge: draft.challenge ?? prev.challenge,
                solution: draft.solution ?? prev.solution,
                results: draft.results ?? prev.results,
                keyMetrics: draft.keyMetrics ?? prev.keyMetrics,
                timeline: draft.timeline ?? prev.timeline,
                testimonial: draft.testimonial ?? prev.testimonial,
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
            setFieldErrors({ title: "Enter a title before generating the deck." });
            return;
        }
        setFieldErrors({});
        setSubmitted(null);
        createMutation.mutate(form);
    }

    function handleFilesChosen(e: React.ChangeEvent<HTMLInputElement>) {
        const chosen = Array.from(e.target.files ?? []);
        e.target.value = "";
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

    function removeFile(index: number) {
        setFileError(null);
        setSourceFiles((prev) => prev.filter((_, i) => i !== index));
    }

    function handleAutoDraft() {
        if (sourceFiles.length === 0) return;
        setFileError(null);
        draftMutation.mutate(sourceFiles);
    }

    return (
        <AppShell appName="Strategy & Planning">
            <div className="csb">
                <div className="csb__nav">
                    <button type="button" className="csb__back" onClick={() => navigate("/")}>
                        ← Back
                    </button>
                    <span className="csb__breadcrumb">Strategy / Documents</span>
                </div>

                <h1 className="csb__title">Case Study Builder</h1>
                <p className="csb__subtitle">Generate case studies from templates.</p>

                <div className="csb__layout">
                    <div className="csb__main">
                        <form className="csb__form" onSubmit={handleSubmit}>
                            <div className="csb__auto-draft">
                                <div className="csb__auto-draft-label">
                                    <span className="csb__auto-draft-icon" aria-hidden="true">⬆</span>
                                    <div>
                                        <span className="csb__auto-draft-title">Auto-Draft</span>
                                        <span className="csb__auto-draft-sub">
                                            Upload up to {MAX_SOURCE_FILES} files (PDF, PowerPoint, Excel, CSV)
                                        </span>
                                    </div>
                                </div>
                                <label className="csb__upload-btn">
                                    Add Files
                                    <input
                                        type="file"
                                        multiple
                                        accept=".pdf,.pptx,.ppt,.xlsx,.xls,.csv"
                                        className="csb__file-input"
                                        onChange={handleFilesChosen}
                                    />
                                </label>
                            </div>

                            {sourceFiles.length > 0 && (
                                <ul className="csb__file-list">
                                    {sourceFiles.map((file, index) => (
                                        <li key={`${file.name}-${file.size}`} className="csb__file-item">
                                            <span className="csb__file-name">{file.name}</span>
                                            <button
                                                type="button"
                                                className="csb__file-remove"
                                                aria-label={`Remove ${file.name}`}
                                                onClick={() => removeFile(index)}
                                            >
                                                ✕
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}

                            {fileError && <ErrorAlert message={fileError} />}

                            {sourceFiles.length > 0 && (
                                <div className="csb__actions csb__actions--draft">
                                    <button
                                        type="button"
                                        className="csb__submit"
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
                                <p className="csb__file-chosen">
                                    Draft ready — review and edit the fields below, then generate the deck.
                                </p>
                            )}

                            <div className="csb__divider">
                                <span className="csb__divider-text">OR FILL DETAILS</span>
                            </div>

                            <div className="csb__fields">
                                <div className="csb__field">
                                    <label className="csb__label" htmlFor="csb-title">Title <span className="csb__required">*</span></label>
                                    <input
                                        id="csb-title"
                                        type="text"
                                        className="csb__input"
                                        value={form.title}
                                        onChange={(e) => handleChange("title", e.target.value)}
                                        placeholder="e.g. Acme Corp Digital Transformation"
                                        required
                                        aria-invalid={Boolean(fieldErrors.title)}
                                        aria-describedby={fieldErrors.title ? "csb-title-error" : undefined}
                                    />
                                    {fieldErrors.title && (
                                        <p className="csb__field-error" id="csb-title-error" role="alert">
                                            {fieldErrors.title}
                                        </p>
                                    )}
                                </div>

                                <div className="csb__field-row">
                                    <div className="csb__field">
                                        <label className="csb__label" htmlFor="csb-client">Client Name</label>
                                        <input
                                            id="csb-client"
                                            type="text"
                                            className="csb__input"
                                            value={form.clientName}
                                            onChange={(e) => handleChange("clientName", e.target.value)}
                                            placeholder="e.g. Acme Corp"
                                        />
                                    </div>
                                    <div className="csb__field">
                                        <label className="csb__label" htmlFor="csb-industry">Industry</label>
                                        <input
                                            id="csb-industry"
                                            type="text"
                                            className="csb__input"
                                            value={form.industry}
                                            onChange={(e) => handleChange("industry", e.target.value)}
                                            placeholder="e.g. Financial Services"
                                        />
                                    </div>
                                </div>

                                <div className="csb__field">
                                    <label className="csb__label" htmlFor="csb-challenge">Challenge</label>
                                    <textarea
                                        id="csb-challenge"
                                        className="csb__textarea"
                                        value={form.challenge}
                                        onChange={(e) => handleChange("challenge", e.target.value)}
                                        placeholder="Describe the client's challenge or problem statement..."
                                        rows={3}
                                    />
                                </div>

                                <div className="csb__field">
                                    <label className="csb__label" htmlFor="csb-solution">Solution</label>
                                    <textarea
                                        id="csb-solution"
                                        className="csb__textarea"
                                        value={form.solution}
                                        onChange={(e) => handleChange("solution", e.target.value)}
                                        placeholder="Describe the solution or approach delivered..."
                                        rows={3}
                                    />
                                </div>

                                <div className="csb__field">
                                    <label className="csb__label" htmlFor="csb-results">Results</label>
                                    <textarea
                                        id="csb-results"
                                        className="csb__textarea"
                                        value={form.results}
                                        onChange={(e) => handleChange("results", e.target.value)}
                                        placeholder="Describe the outcomes and results achieved..."
                                        rows={3}
                                    />
                                </div>

                                <div className="csb__field-row">
                                    <div className="csb__field">
                                        <label className="csb__label" htmlFor="csb-metrics">Key Metrics</label>
                                        <input
                                            id="csb-metrics"
                                            type="text"
                                            className="csb__input"
                                            value={form.keyMetrics}
                                            onChange={(e) => handleChange("keyMetrics", e.target.value)}
                                            placeholder="e.g. 40% cost reduction, 2x pipeline"
                                        />
                                    </div>
                                    <div className="csb__field">
                                        <label className="csb__label" htmlFor="csb-timeline">Timeline</label>
                                        <input
                                            id="csb-timeline"
                                            type="text"
                                            className="csb__input"
                                            value={form.timeline}
                                            onChange={(e) => handleChange("timeline", e.target.value)}
                                            placeholder="e.g. 6 months, Q1–Q3 2025"
                                        />
                                    </div>
                                </div>

                                <div className="csb__field">
                                    <label className="csb__label" htmlFor="csb-testimonial">Testimonial / Quote</label>
                                    <textarea
                                        id="csb-testimonial"
                                        className="csb__textarea"
                                        value={form.testimonial}
                                        onChange={(e) => handleChange("testimonial", e.target.value)}
                                        placeholder="Client quote to feature in the deck..."
                                        rows={2}
                                    />
                                </div>
                            </div>

                            {createMutation.isError && (
                                <ErrorAlert
                                    message={
                                        createMutation.error instanceof Error && createMutation.error.message
                                            ? createMutation.error.message
                                            : "Failed to generate the deck. Please try again."
                                    }
                                />
                            )}

                            {submitted && (
                                <div className="csb__success">
                                    <span className="csb__success-icon" aria-hidden="true">✓</span>
                                    <div>
                                        <p className="csb__success-title">Deck created!</p>
                                        <p className="csb__success-body">
                                            {submitted.slidesUrl ? (
                                                <>
                                                    Your case study deck is ready in your Google Drive.{" "}
                                                    <a
                                                        href={submitted.slidesUrl}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="csb__template-link"
                                                    >
                                                        Open in Google Slides
                                                    </a>
                                                    .
                                                </>
                                            ) : (
                                                "Your case study has been saved."
                                            )}
                                        </p>
                                    </div>
                                </div>
                            )}

                            <div className="csb__actions">
                                <button
                                    type="submit"
                                    className="csb__submit"
                                    disabled={!form.title.trim() || createMutation.isPending}
                                >
                                    {createMutation.isPending ? "Generating…" : "Generate Deck"}
                                </button>
                            </div>
                        </form>
                    </div>

                    <aside className="csb__aside">
                        <div className="csb__recent">
                            <h2 className="csb__recent-title">Recent Studies</h2>
                            {recentQuery.isLoading && <LoadingBlock />}
                            {recentQuery.isError && <p className="csb__recent-empty">Could not load recent studies.</p>}
                            {recentQuery.data?.length === 0 && (
                                <p className="csb__recent-empty">No recent case studies.</p>
                            )}
                            {recentQuery.data && recentQuery.data.length > 0 && (
                                <ul className="csb__recent-list">
                                    {recentQuery.data.map((study) => (
                                        <li key={study.id} className="csb__recent-item">
                                            <span className="csb__recent-name">{study.title}</span>
                                            {study.clientName && (
                                                <span className="csb__recent-meta">{study.clientName}</span>
                                            )}
                                            <span className={`csb__recent-badge csb__recent-badge--${study.status?.toLowerCase()}`}>
                                                {study.status === "GENERATED"
                                                    ? "Generated"
                                                    : study.status === "SUBMITTED"
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
