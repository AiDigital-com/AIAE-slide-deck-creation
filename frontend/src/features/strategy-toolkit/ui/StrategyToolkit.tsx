import { useNavigate } from "react-router-dom";

import { TOOLS } from "../constants/tools";
import "./strategy-toolkit.css";

/** Strategy Toolkit home page — tool card grid. */
export function StrategyToolkit() {
    const navigate = useNavigate();

    return (
        <div className="strategy-toolkit">
            <div className="strategy-toolkit__header">
                <h1 className="strategy-toolkit__title">Strategy Toolkit</h1>
                <p className="strategy-toolkit__subtitle">
                    Internal tools for creating client deliverables. Select a tool to begin your workflow.
                </p>
            </div>

            <div className="strategy-toolkit__grid">
                {TOOLS.map((tool) => {
                    const isExternal = Boolean(tool.externalUrl);
                    const cardContent = (
                        <>
                            <div className={`tool-card__icon-wrap${tool.available ? "" : " tool-card__icon-wrap--muted"}`}>
                                <span className="tool-card__icon" role="img" aria-label={tool.title}>
                                    {tool.icon}
                                </span>
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
                        <span className="tool-card__icon tool-card__icon--plus" aria-hidden="true">+</span>
                    </div>
                    <h2 className="tool-card__title tool-card__title--muted">More Tools</h2>
                    <p className="tool-card__description">Coming soon to the suite.</p>
                </div>
            </div>
        </div>
    );
}
