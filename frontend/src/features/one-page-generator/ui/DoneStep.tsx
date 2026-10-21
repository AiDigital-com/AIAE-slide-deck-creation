import type { DoneStepProps } from "../model/types";

/** The done step: the page lives in the user's Drive as a new, fully editable Slides file. */
export function DoneStep({ slidesUrl, onStartOver }: DoneStepProps) {
    return (
        <div className="opg__card-panel">
            <div className="opg__success">
                <span className="opg__success-icon" aria-hidden="true">✓</span>
                <div className="opg__success-text">
                    <p className="opg__success-title">Page created!</p>
                    <p className="opg__success-body">Your one-pager is ready in your Google Drive as a new, fully editable Slides file.</p>
                </div>
            </div>
            <div className="opg__actions">
                <button type="button" className="opg__secondary" onClick={onStartOver}>
                    Create another page
                </button>
                {slidesUrl && (
                    <a className="opg__slides-link" href={slidesUrl} target="_blank" rel="noopener noreferrer">
                        Open in Google Slides →
                    </a>
                )}
            </div>
        </div>
    );
}
