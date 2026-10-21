import { useNavigate } from "react-router-dom";

import { AppShell } from "../../../app/AppShell";
import { CASE_STUDY_TEMPLATES } from "../constants/templates";
import "./case-study-builder.css";

/** Template picker shown before either Case Study deck-generation flow. */
export function CaseStudyTemplatePickerPage() {
    const navigate = useNavigate();

    return (
        <AppShell appName="Strategy Toolkit">
            <div className="csb">
                <div className="csb__nav">
                    <button type="button" className="csb__back" onClick={() => navigate("/")}>
                        ← Back
                    </button>
                    <span className="csb__breadcrumb">Strategy / Case Study</span>
                </div>

                <h1 className="csb__title">Case Study Builder</h1>
                <p className="csb__subtitle">
                    Choose a template to get started. Both draft the content from your files and create a
                    ready-to-edit deck in your own Google Slides.
                </p>

                <div className="csb__template-grid">
                    {CASE_STUDY_TEMPLATES.map((template) => (
                        <button
                            key={template.key}
                            type="button"
                            className="csb__template-card"
                            onClick={() => navigate(template.route)}
                        >
                            <span className="csb__template-title">{template.title}</span>
                            <span className="csb__template-desc">{template.description}</span>
                            <span className="csb__template-cta">Get started →</span>
                        </button>
                    ))}
                </div>
            </div>
        </AppShell>
    );
}
