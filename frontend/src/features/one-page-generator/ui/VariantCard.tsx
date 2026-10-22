import type { VariantCardProps } from "../model/types";

/**
 * A shell (radio) or module (checkbox) picker card. Thumbnails span very different aspect
 * ratios, so they are letterboxed rather than cropped.
 */
export function VariantCard({ name, kind, checked, disabled, label, description, thumbnail, shell, onToggle }: VariantCardProps) {
    return (
        <label
            className={`opg__card${checked ? " opg__card--checked" : ""}${disabled ? " opg__card--disabled" : ""}`}
        >
            <span className={`opg__thumb${shell ? " opg__thumb--shell" : ""}`}>
                <img className="opg__thumb-img" src={thumbnail} alt="" loading="lazy" />
                <input
                    type={kind}
                    name={name}
                    className="opg__card-input"
                    checked={checked}
                    disabled={disabled}
                    onChange={(e) => onToggle(e.target.checked)}
                />
            </span>
            <span className="opg__card-caption">
                <span className="opg__card-title">{label}</span>
                {description && <span className="opg__card-desc">{description}</span>}
            </span>
        </label>
    );
}
