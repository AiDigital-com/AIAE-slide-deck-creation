import { KPI_TILE_COUNT, MODULE_PREVIEWS, SOLUTION_CARD_COUNT } from "../constants/modules";
import type { ModulePreviewProps } from "../model/types";

/**
 * Live preview of a module inside a canvas block: real crops from the master template. Solutions
 * and KPIs repeat one cropped unit at the real assembly spacing, since assembly restretches those
 * two from the master's native spacing; everything else is the master's own row.
 */
export function ModulePreview({ moduleKey }: ModulePreviewProps) {
    const src = MODULE_PREVIEWS[moduleKey];
    if (moduleKey === "SOLUTIONS") {
        return (
            <div className="opg__pv opg__pv--solution-stack">
                {Array.from({ length: SOLUTION_CARD_COUNT }, (_, i) => (
                    <img key={i} src={src} alt="" className="opg__pv-solution-card" />
                ))}
            </div>
        );
    }
    if (moduleKey === "KPIS") {
        return (
            <div className="opg__pv opg__pv--kpi-row">
                {Array.from({ length: KPI_TILE_COUNT }, (_, i) => (
                    <img key={i} src={src} alt="" className="opg__pv-kpi-tile" />
                ))}
            </div>
        );
    }
    if (moduleKey === "IMAGE_TEXT") {
        return (
            <div className="opg__pv opg__pv--image-text">
                <img src={src} alt="" className="opg__pv-image-text-top" />
                <div className="opg__pv-image-text-bottom" />
            </div>
        );
    }
    return (
        <div className="opg__pv">
            <img src={src} alt="" className="opg__pv-photo" />
        </div>
    );
}
