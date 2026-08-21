import type { ToolCard } from "../model/types";

/** The tool cards rendered on the Strategy Toolkit home page, in display order. */
export const TOOLS: ToolCard[] = [
    {
        icon: "📄",
        title: "Case Study Builder",
        description: "Generate case studies from Google Slides templates using uploaded reports or manual entry.",
        route: "/case-study-builder",
        available: true,
    },
    {
        icon: "📊",
        title: "Category Analysis Builder",
        description: "Assemble 3–5 slide category analyses and publish to Google Slides.",
        route: "/category-analysis-builder",
        available: true,
    },
    {
        icon: "🎯",
        title: "RFP Outline Generator",
        description: "Upload an RFP and draft a succinct POV plus a response deck outline, exported to Google Docs.",
        route: "/rfp-outline-generator",
        available: true,
    },
    {
        icon: "🧭",
        title: "Product Redirection Script",
        description: "Triage growth-team requests by deal size — escalate $100k+ buys to RnD or draft a relay-ready workaround response.",
        externalUrl: "https://aidigital.slite.com/app/super/assistants/SuWgAiRUINCqVK",
        available: true,
    },
];
