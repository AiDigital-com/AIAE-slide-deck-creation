import { useNavigate } from "react-router-dom";

import { AppShell } from "../../../app/AppShell";
import { CATEGORY_ANALYSIS_TEMPLATES } from "../constants/templates";
import "./category-analysis-builder.css";

/** Template picker shown before either Category Analysis deck-generation flow. */
export function TemplatePickerPage() {
    const navigate = useNavigate();

    return (
        <AppShell appName="Strategy Toolkit">
            <div className="cab">
                <div className="cab__nav">
                    <button type="button" className="cab__back" onClick={() => navigate("/")}>
                        ← Back
                    </button>
                    <span className="cab__breadcrumb">Strategy / Category Analysis</span>
                </div>

                <h1 className="cab__title">Category Analysis Builder</h1>
                <p className="cab__subtitle">
                    Choose a template to get started. Both AI-draft the content and create a
                    ready-to-edit deck in your own Google Slides.
                </p>

                <div className="cab__template-grid">
                    {CATEGORY_ANALYSIS_TEMPLATES.map((template) => (
                        <button
                            key={template.key}
                            type="button"
                            className="cab__template-card"
                            onClick={() => navigate(template.route)}
                        >
                            <span className="cab__template-title">{template.title}</span>
                            <span className="cab__template-desc">{template.description}</span>
                            <span className="cab__template-cta">Get started →</span>
                        </button>
                    ))}
                </div>
            </div>
        </AppShell>
    );
}
