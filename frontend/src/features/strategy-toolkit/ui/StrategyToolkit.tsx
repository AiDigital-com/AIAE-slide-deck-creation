import { useNavigate } from "react-router-dom";

import { COMING_SOON_TOOL_ICON, TOOLS } from "../constants/tools";
import "./strategy-toolkit.css";

/** Strategy Toolkit home page — brand hero band over the tool card grid. */
export function StrategyToolkit() {
    const navigate = useNavigate();

    return (
        <div className="strategy-toolkit">
            <div className="strategy-toolkit__hero">
                <svg
                    className="strategy-toolkit__wave"
                    viewBox="0 0 1200 120"
                    preserveAspectRatio="none"
                    aria-hidden="true"
                >
                    <path d="M0 96 C 180 -10, 360 -10, 540 60 S 900 130, 1200 20" />
                </svg>
                <div className="strategy-toolkit__header">
                    <span className="strategy-toolkit__kicker">AI Digital · Strategy</span>
                    <h1 className="strategy-toolkit__title">Strategy Toolkit</h1>
                    <p className="strategy-toolkit__subtitle">
                        Internal tools for creating client deliverables. Select a tool to begin your workflow.
                    </p>
                </div>
            </div>

            <div className="strategy-toolkit__grid">
                {TOOLS.map((tool) => {
                    const isExternal = Boolean(tool.externalUrl);
                    const cardContent = (
                        <>
                            <div className={`tool-card__icon-wrap${tool.available ? "" : " tool-card__icon-wrap--muted"}`}>
                                <img className="tool-card__icon" src={tool.icon} alt="" />
                            </div>
                            <h2 className={`tool-card__title${tool.available ? "" : " tool-card__title--muted"}`}>
                                {tool.title}
                            </h2>
                            <p className="tool-card__description">{tool.description}</p>
                            {tool.available ? (
                                <span className="tool-card__link">
                                    {isExternal ? "Open external tool" : "Open tool"}{" "}
                                    <span aria-hidden="true">{isExternal ? "↗" : "→"}</span>
                                </span>
                            ) : (
                                <span className="tool-card__link tool-card__link--disabled">Temporarily disabled</span>
                            )}
                        </>
                    );

                    if (isExternal && tool.available) {
                        return (
                            <a
                                key={tool.externalUrl}
                                className="tool-card"
                                href={tool.externalUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                {cardContent}
                            </a>
                        );
                    }

                    return (
                        <div
                            key={tool.route}
                            className={`tool-card${tool.available ? "" : " tool-card--disabled"}`}
                            role={tool.available ? "button" : undefined}
                            tabIndex={tool.available ? 0 : undefined}
                            onClick={tool.available ? () => navigate(tool.route!) : undefined}
                            onKeyDown={
                                tool.available
                                    ? (event) => {
                                          if (event.key === "Enter" || event.key === " ") {
                                              event.preventDefault();
                                              navigate(tool.route!);
                                          }
                                      }
                                    : undefined
                            }
                        >
                            {cardContent}
                        </div>
                    );
                })}

                <div className="tool-card tool-card--coming-soon">
                    <div className="tool-card__icon-wrap tool-card__icon-wrap--muted">
                        <img className="tool-card__icon" src={COMING_SOON_TOOL_ICON} alt="" />
                    </div>
                    <h2 className="tool-card__title tool-card__title--muted">More tools coming</h2>
                    <p className="tool-card__description">New strategy tools appear here as they ship.</p>
                    <span className="tool-card__link tool-card__link--disabled">Coming soon</span>
                </div>
            </div>
        </div>
    );
}
