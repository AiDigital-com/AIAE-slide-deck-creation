import type { ToolCard } from "../model/types";

/** Icon shown on the non-interactive "more tools coming" placeholder card. */
export const COMING_SOON_TOOL_ICON = "/tool-icons/coming-soon.png";

/** The tool cards rendered on the Strategy Toolkit home page, in display order. */
export const TOOLS: ToolCard[] = [
    {
        icon: "/tool-icons/case-study.png",
        title: "Case Study Builder",
        description: "Generate case studies from Google Slides templates using uploaded reports or manual entry.",
        route: "/case-study-builder",
        available: true,
    },
    {
        icon: "/tool-icons/category-analysis.png",
        title: "Category Analysis Builder",
        description: "Assemble 3–5 slide category analyses and publish to Google Slides.",
        route: "/category-analysis-builder",
        available: true,
    },
    {
        icon: "/tool-icons/rfp-outline.png",
        title: "RFP Outline Generator",
        description: "Upload an RFP and draft a succinct POV plus a response deck outline, exported to Google Docs.",
        route: "/rfp-outline-generator",
        available: true,
    },
    {
        icon: "/tool-icons/redirection.png",
        title: "Product Redirection Script",
        description: "Triage growth-team requests by deal size — escalate $100k+ buys to RnD or draft a relay-ready workaround response.",
        externalUrl: "https://aidigital.slite.com/app/super/assistants/SuWgAiRUINCqVK",
        available: true,
    },
    {
        icon: "/tool-icons/one-pager.png",
        title: "One Page Generator",
        description: "Brief a vertical one-pager and get a ready-to-edit single-slide page in Google Slides, assembled from the master template's header/footer shell and body modules.",
        route: "/one-page-generator",
        available: true,
    },
];
