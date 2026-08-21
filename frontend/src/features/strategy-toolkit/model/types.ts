/** One tool card on the Strategy Toolkit home page. */
export interface ToolCard {
    icon: string;
    title: string;
    description: string;
    /** Internal app route. Omit when the tool is hosted externally — use `externalUrl` instead. */
    route?: string;
    /** External URL to link to when this tool is hosted outside this app. Opens in a new tab. */
    externalUrl?: string;
    available: boolean;
}
