import { ErrorAlert } from "../../../shared/ui/ErrorAlert";
import { GUIDANCE_NOTES_ROWS } from "../constants/form";
import { MODULE_OPTIONS, SHELLS } from "../constants/modules";
import { isModuleActive } from "../model/moduleOrder";
import type { BriefFormProps } from "../model/types";
import { VariantCard } from "./VariantCard";

/**
 * The setup step: pick a shell and the body modules, then brief the drafter. Every module is a
 * known-good, pre-sized layout; the brief is the only source of facts the drafter uses.
 */
export function BriefForm({
    brief,
    errors,
    aiConnected,
    connectionsLoaded,
    drafting,
    draftError,
    onBriefChange,
    onShellChange,
    onModuleToggle,
    onSubmit,
}: BriefFormProps) {
    const blockers: string[] = [];
    if (connectionsLoaded && !aiConnected) blockers.push("AI drafting isn't connected");
    if (!brief.category.trim()) blockers.push("Category / vertical is empty");
    if (!brief.clientName.trim()) blockers.push("Client / label is empty");
    if (!brief.guidanceNotes.trim()) blockers.push("Guidance brief is empty");

    return (
        <form
            className="opg__card-panel"
            onSubmit={(e) => {
                e.preventDefault();
                onSubmit();
            }}
            noValidate
        >
            {connectionsLoaded && !aiConnected && (
                <div className="opg__notice opg__notice--warning">
                    <p className="opg__notice-title">AI drafting is not connected</p>
                    <p className="opg__notice-body">
                        Ask your admin to add the OpenAI API key so the builder can draft page content for you.
                    </p>
                </div>
            )}

            <fieldset className="opg__fieldset">
                <legend className="opg__legend">Header / footer shell</legend>
                <div className="opg__picker opg__picker--shells">
                    {SHELLS.map((shell) => (
                        <VariantCard
                            key={shell.value}
                            name="shell"
                            kind="radio"
                            checked={brief.shellVariant === shell.value}
                            label={shell.label}
                            thumbnail={shell.thumbnail}
                            shell
                            onToggle={() => onShellChange(shell.value)}
                        />
                    ))}
                </div>
                {brief.shellVariant === "HERO_GRAPHIC" && (
                    <p className="opg__hint">
                        Shell 2's large hero graphic leaves less body room; a page with many modules runs long.
                    </p>
                )}
            </fieldset>

            <fieldset className="opg__fieldset">
                <legend className="opg__legend">Body modules</legend>
                <p className="opg__hint">
                    Check the modules the page should carry. Drag them in the live outline to set their order.
                </p>
                <div className="opg__picker">
                    {MODULE_OPTIONS.map((option) => {
                        const requirementMet = !option.requires || isModuleActive(brief.moduleOrder, option.requires);
                        return (
                            <VariantCard
                                key={option.key}
                                name={`module-${option.key}`}
                                kind="checkbox"
                                checked={isModuleActive(brief.moduleOrder, option.key)}
                                disabled={!requirementMet}
                                label={option.title}
                                description={option.description}
                                thumbnail={option.thumbnail}
                                onToggle={(checked) => onModuleToggle(option.key, checked)}
                            />
                        );
                    })}
                </div>
            </fieldset>

            <div className="opg__fields">
                <div className="opg__field">
                    <label className="opg__label" htmlFor="opg-category">
                        Category / vertical <span className="opg__required">*</span>
                    </label>
                    <input
                        id="opg-category"
                        type="text"
                        className={`opg__input${errors.category ? " opg__input--invalid" : ""}`}
                        value={brief.category}
                        placeholder="e.g. Automotive"
                        required
                        aria-invalid={Boolean(errors.category)}
                        aria-describedby={errors.category ? "opg-category-error" : undefined}
                        onChange={(e) => onBriefChange("category", e.target.value)}
                    />
                    {errors.category && (
                        <p className="opg__field-error" id="opg-category-error" role="alert">
                            {errors.category}
                        </p>
                    )}
                </div>

                <div className="opg__field">
                    <label className="opg__label" htmlFor="opg-client">
                        Client / label <span className="opg__required">*</span>
                    </label>
                    <input
                        id="opg-client"
                        type="text"
                        className={`opg__input${errors.clientName ? " opg__input--invalid" : ""}`}
                        value={brief.clientName}
                        placeholder="Used in the file's title"
                        required
                        aria-invalid={Boolean(errors.clientName)}
                        aria-describedby={errors.clientName ? "opg-client-error" : undefined}
                        onChange={(e) => onBriefChange("clientName", e.target.value)}
                    />
                    {errors.clientName && (
                        <p className="opg__field-error" id="opg-client-error" role="alert">
                            {errors.clientName}
                        </p>
                    )}
                </div>

                <div className="opg__field">
                    <label className="opg__label" htmlFor="opg-notes">
                        Guidance brief <span className="opg__required">*</span>
                    </label>
                    <textarea
                        id="opg-notes"
                        className={`opg__textarea${errors.guidanceNotes ? " opg__input--invalid" : ""}`}
                        value={brief.guidanceNotes}
                        rows={GUIDANCE_NOTES_ROWS}
                        placeholder="Paste the full brief: headline, solution copy, KPI values, disclaimers. Everything factual must be here; nothing is researched or invented."
                        required
                        aria-invalid={Boolean(errors.guidanceNotes)}
                        aria-describedby={errors.guidanceNotes ? "opg-notes-error" : "opg-notes-help"}
                        onChange={(e) => onBriefChange("guidanceNotes", e.target.value)}
                    />
                    {errors.guidanceNotes ? (
                        <p className="opg__field-error" id="opg-notes-error" role="alert">
                            {errors.guidanceNotes}
                        </p>
                    ) : (
                        <p className="opg__hint" id="opg-notes-help">
                            The drafter never uses web search. It only tightens wording to fit the modules above; any
                            number, claim, or name must already be in this brief.
                        </p>
                    )}
                </div>
            </div>

            {draftError && <ErrorAlert message={draftError} />}

            {blockers.length > 0 && !drafting && (
                <p className="opg__blocking" role="status">
                    Can't draft yet: {blockers.join(", ")}.
                </p>
            )}

            <div className="opg__actions">
                <button type="submit" className="opg__submit" disabled={blockers.length > 0 || drafting}>
                    {drafting ? "Drafting content…" : "Draft the page"}
                </button>
            </div>
        </form>
    );
}
