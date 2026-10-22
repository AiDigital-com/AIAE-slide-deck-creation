import { ErrorAlert } from "../../../shared/ui/ErrorAlert";
import { LoadingBlock } from "../../../shared/ui/LoadingBlock";
import { MAX_CARD_BULLETS, MAX_TITLE_LINES } from "../constants/form";
import { replaceAt, splitLines } from "../model/review";
import type { DraftReviewProps, OnePagerDraft, ReviewFieldProps } from "../model/types";
import { GoogleAccessNotice } from "./GoogleAccessNotice";

/** One labelled text control of the review form. */
function ReviewField({ id, label, value, multiline, rows, onChange }: ReviewFieldProps) {
    return (
        <div className="opg__field">
            <label className="opg__label" htmlFor={id}>
                {label}
            </label>
            {multiline ? (
                <textarea id={id} className="opg__textarea" rows={rows ?? 2} value={value} onChange={(e) => onChange(e.target.value)} />
            ) : (
                <input id={id} type="text" className="opg__input" value={value} onChange={(e) => onChange(e.target.value)} />
            )}
        </div>
    );
}

/**
 * The review step: every drafted field of every included module is editable before the page is
 * created. A module is included by its content, so clearing a module's text leaves it out.
 */
export function DraftReview({ draft, googleStatus, connectionsLoaded, creating, createError, onChange, onStartOver, onCreate }: DraftReviewProps) {
    const googleConnected = googleStatus === "CONNECTED";

    function patch(changes: Partial<OnePagerDraft>) {
        onChange({ ...draft, ...changes });
    }

    return (
        <div className="opg__card-panel">
            <div className="opg__review-head">
                <h2 className="opg__review-title">Review the draft</h2>
                <p className="opg__review-sub">
                    Edit anything below before creating the page. Check every figure and claim against your brief.
                </p>
            </div>

            <section className="opg__section">
                <h3 className="opg__section-title">Header</h3>
                <ReviewField id="opg-topic-pill" label="Topic pill" value={draft.topicPill} onChange={(v) => patch({ topicPill: v })} />
                <ReviewField
                    id="opg-main-title"
                    label={`Main title (one line per row, up to ${MAX_TITLE_LINES})`}
                    value={draft.mainTitle.join("\n")}
                    multiline
                    rows={MAX_TITLE_LINES}
                    onChange={(v) => patch({ mainTitle: splitLines(v, MAX_TITLE_LINES) })}
                />
                <ReviewField id="opg-supporting" label="Supporting copy" value={draft.supportingCopy} onChange={(v) => patch({ supportingCopy: v })} />
            </section>

            {draft.processSteps.length > 0 && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Process tracker</h3>
                    <ReviewField id="opg-process-label" label="Section label" value={draft.processSectionLabel ?? ""} onChange={(v) => patch({ processSectionLabel: v })} />
                    {draft.processSteps.map((step, i) => (
                        <ReviewField
                            key={i}
                            id={`opg-step-${i}`}
                            label={`Step ${i + 1}`}
                            value={step}
                            onChange={(v) => patch({ processSteps: replaceAt(draft.processSteps, i, v) })}
                        />
                    ))}
                </section>
            )}

            {draft.solutions.length > 0 && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Core solutions</h3>
                    <ReviewField id="opg-solutions-label" label="Section label" value={draft.solutionsSectionLabel ?? ""} onChange={(v) => patch({ solutionsSectionLabel: v })} />
                    {draft.solutions.map((solution, i) => (
                        <div className="opg__group" key={i}>
                            <h4 className="opg__group-title">Solution {i + 1}</h4>
                            <ReviewField id={`opg-solution-${i}-title`} label="Title" value={solution.title} onChange={(v) => patch({ solutions: replaceAt(draft.solutions, i, { ...solution, title: v }) })} />
                            <ReviewField id={`opg-solution-${i}-value`} label="Value line" value={solution.valueLine} onChange={(v) => patch({ solutions: replaceAt(draft.solutions, i, { ...solution, valueLine: v }) })} />
                            <ReviewField id={`opg-solution-${i}-body`} label="Body" value={solution.body} multiline onChange={(v) => patch({ solutions: replaceAt(draft.solutions, i, { ...solution, body: v }) })} />
                        </div>
                    ))}
                    {draft.solutionsVisualLabel != null && (
                        <ReviewField id="opg-visual-label" label="Solutions visual label" value={draft.solutionsVisualLabel} onChange={(v) => patch({ solutionsVisualLabel: v })} />
                    )}
                </section>
            )}

            {draft.kpis.length > 0 && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Performance signals</h3>
                    <ReviewField id="opg-kpi-label" label="Section label" value={draft.kpiSectionLabel ?? ""} onChange={(v) => patch({ kpiSectionLabel: v })} />
                    {draft.kpis.map((kpi, i) => (
                        <div className="opg__pair" key={i}>
                            <ReviewField id={`opg-kpi-${i}-value`} label={`KPI ${i + 1} value`} value={kpi.value} onChange={(v) => patch({ kpis: replaceAt(draft.kpis, i, { ...kpi, value: v }) })} />
                            <ReviewField id={`opg-kpi-${i}-label`} label={`KPI ${i + 1} label`} value={kpi.label} onChange={(v) => patch({ kpis: replaceAt(draft.kpis, i, { ...kpi, label: v }) })} />
                        </div>
                    ))}
                    <ReviewField id="opg-kpi-disclaimer" label="Disclaimer" value={draft.kpiDisclaimer ?? ""} multiline onChange={(v) => patch({ kpiDisclaimer: v })} />
                </section>
            )}

            {draft.imageTextBody != null && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Image + text</h3>
                    <ReviewField id="opg-image-text-label" label="Section label" value={draft.imageTextSectionLabel ?? ""} onChange={(v) => patch({ imageTextSectionLabel: v })} />
                    <ReviewField id="opg-image-text-body" label="Body" value={draft.imageTextBody} multiline rows={3} onChange={(v) => patch({ imageTextBody: v })} />
                </section>
            )}

            {draft.keyTakeaway != null && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Key takeaway</h3>
                    <ReviewField id="opg-takeaway" label="Callout" value={draft.keyTakeaway} multiline onChange={(v) => patch({ keyTakeaway: v })} />
                </section>
            )}

            {draft.impactStats.length > 0 && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Impact stats</h3>
                    {draft.impactStats.map((stat, i) => (
                        <div className="opg__pair" key={i}>
                            <ReviewField id={`opg-stat-${i}-value`} label={`Stat ${i + 1} value`} value={stat.value} onChange={(v) => patch({ impactStats: replaceAt(draft.impactStats, i, { ...stat, value: v }) })} />
                            <ReviewField id={`opg-stat-${i}-desc`} label={`Stat ${i + 1} description`} value={stat.description} onChange={(v) => patch({ impactStats: replaceAt(draft.impactStats, i, { ...stat, description: v }) })} />
                        </div>
                    ))}
                </section>
            )}

            {draft.capabilityCards.length > 0 && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Capability cards</h3>
                    {draft.capabilityCards.map((card, i) => (
                        <div className="opg__group" key={i}>
                            <h4 className="opg__group-title">Card {i + 1}</h4>
                            <ReviewField id={`opg-cap-${i}-title`} label="Title" value={card.title} onChange={(v) => patch({ capabilityCards: replaceAt(draft.capabilityCards, i, { ...card, title: v }) })} />
                            <ReviewField id={`opg-cap-${i}-desc`} label="Description" value={card.description} onChange={(v) => patch({ capabilityCards: replaceAt(draft.capabilityCards, i, { ...card, description: v }) })} />
                            <ReviewField id={`opg-cap-${i}-bullets`} label={`Bullets (one per line, up to ${MAX_CARD_BULLETS})`} value={card.bullets.join("\n")} multiline rows={MAX_CARD_BULLETS} onChange={(v) => patch({ capabilityCards: replaceAt(draft.capabilityCards, i, { ...card, bullets: splitLines(v, MAX_CARD_BULLETS) }) })} />
                            <ReviewField id={`opg-cap-${i}-footer`} label="Footer" value={card.footer} onChange={(v) => patch({ capabilityCards: replaceAt(draft.capabilityCards, i, { ...card, footer: v }) })} />
                        </div>
                    ))}
                </section>
            )}

            {draft.spotlightCards.length > 0 && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Spotlight cards</h3>
                    {draft.spotlightCards.map((card, i) => (
                        <div className="opg__group" key={i}>
                            <h4 className="opg__group-title">Card {i + 1}</h4>
                            <ReviewField id={`opg-spot-${i}-title`} label="Title" value={card.title} onChange={(v) => patch({ spotlightCards: replaceAt(draft.spotlightCards, i, { ...card, title: v }) })} />
                            <ReviewField id={`opg-spot-${i}-desc`} label="Description" value={card.description} onChange={(v) => patch({ spotlightCards: replaceAt(draft.spotlightCards, i, { ...card, description: v }) })} />
                            <ReviewField id={`opg-spot-${i}-bullets`} label={`Bullets (one per line, up to ${MAX_CARD_BULLETS})`} value={card.bullets.join("\n")} multiline rows={MAX_CARD_BULLETS} onChange={(v) => patch({ spotlightCards: replaceAt(draft.spotlightCards, i, { ...card, bullets: splitLines(v, MAX_CARD_BULLETS) }) })} />
                            {card.kpiChips.map((chip, c) => (
                                <div className="opg__pair" key={c}>
                                    <ReviewField id={`opg-spot-${i}-chip-${c}-value`} label={`Chip ${c + 1} value`} value={chip.value} onChange={(v) => patch({ spotlightCards: replaceAt(draft.spotlightCards, i, { ...card, kpiChips: replaceAt(card.kpiChips, c, { ...chip, value: v }) }) })} />
                                    <ReviewField id={`opg-spot-${i}-chip-${c}-label`} label={`Chip ${c + 1} label`} value={chip.label} onChange={(v) => patch({ spotlightCards: replaceAt(draft.spotlightCards, i, { ...card, kpiChips: replaceAt(card.kpiChips, c, { ...chip, label: v }) }) })} />
                                </div>
                            ))}
                            {card.stageNames.map((stage, s) => (
                                <ReviewField key={s} id={`opg-spot-${i}-stage-${s}`} label={`Stage ${s + 1}`} value={stage} onChange={(v) => patch({ spotlightCards: replaceAt(draft.spotlightCards, i, { ...card, stageNames: replaceAt(card.stageNames, s, v) }) })} />
                            ))}
                        </div>
                    ))}
                </section>
            )}

            {draft.insightCards.length > 0 && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Insight cards</h3>
                    {draft.insightCards.map((card, i) => (
                        <div className="opg__group" key={i}>
                            <h4 className="opg__group-title">Card {i + 1}</h4>
                            <ReviewField id={`opg-insight-${i}-title`} label="Title" value={card.title} onChange={(v) => patch({ insightCards: replaceAt(draft.insightCards, i, { ...card, title: v }) })} />
                            <ReviewField id={`opg-insight-${i}-text`} label="Insight" value={card.insight} multiline onChange={(v) => patch({ insightCards: replaceAt(draft.insightCards, i, { ...card, insight: v }) })} />
                        </div>
                    ))}
                </section>
            )}

            {draft.milestones.length > 0 && (
                <section className="opg__section">
                    <h3 className="opg__section-title">Milestone banner</h3>
                    <ReviewField id="opg-milestone-headline" label="Headline" value={draft.milestoneHeadline ?? ""} onChange={(v) => patch({ milestoneHeadline: v })} />
                    {draft.milestones.map((milestone, i) => (
                        <div className="opg__pair" key={i}>
                            <ReviewField id={`opg-milestone-${i}-title`} label={`Step ${i + 1} title`} value={milestone.title} onChange={(v) => patch({ milestones: replaceAt(draft.milestones, i, { ...milestone, title: v }) })} />
                            <ReviewField id={`opg-milestone-${i}-subtext`} label={`Step ${i + 1} subtext`} value={milestone.subtext} onChange={(v) => patch({ milestones: replaceAt(draft.milestones, i, { ...milestone, subtext: v }) })} />
                        </div>
                    ))}
                </section>
            )}

            {connectionsLoaded && <GoogleAccessNotice status={googleStatus} />}

            {creating && <LoadingBlock label="Assembling your page and saving it to Google Slides…" />}

            {createError && <ErrorAlert message={createError} />}

            <div className="opg__actions">
                <button type="button" className="opg__secondary" disabled={creating} onClick={onStartOver}>
                    Start over
                </button>
                <button
                    type="button"
                    className="opg__submit"
                    disabled={creating || (connectionsLoaded && !googleConnected)}
                    onClick={onCreate}
                >
                    {creating ? "Creating page…" : "Create Google Slides page"}
                </button>
            </div>
        </div>
    );
}
