import { EmptyState } from "../../../shared/ui/EmptyState";
import { ErrorAlert } from "../../../shared/ui/ErrorAlert";
import { LoadingBlock } from "../../../shared/ui/LoadingBlock";
import { RECENT_PAGES_EMPTY, RECENT_PAGES_LOAD_ERROR } from "../constants/form";
import type { RecentPagesProps } from "../model/types";

/**
 * The caller's recently created one-pagers, each opening its Slides file. Rendered beside the
 * builder so a page created earlier can be reopened without leaving the tool.
 */
export function RecentPages({ pages, loading, error }: RecentPagesProps) {
    return (
        <section className="opg__aside-card opg__recent" aria-labelledby="opg-recent-title">
            <h2 id="opg-recent-title" className="opg__aside-title">
                Recent Pages
            </h2>
            {loading && <LoadingBlock />}
            {!loading && error && <ErrorAlert message={RECENT_PAGES_LOAD_ERROR} />}
            {!loading && !error && pages.length === 0 && <EmptyState message={RECENT_PAGES_EMPTY} />}
            {!loading && !error && pages.length > 0 && (
                <ul className="opg__recent-list">
                    {pages.map((page) => (
                        <li key={page.id} className="opg__recent-item">
                            <span className="opg__recent-name">{page.title}</span>
                            {page.category && <span className="opg__recent-meta">{page.category}</span>}
                            {page.slidesUrl ? (
                                <a className="opg__recent-link" href={page.slidesUrl} target="_blank" rel="noreferrer">
                                    Open page →
                                </a>
                            ) : (
                                <span className="opg__recent-meta">{page.status === "SUBMITTED" ? "Submitted" : "Draft"}</span>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}
