import type { components } from "../../../shared/api/generated/schema";

/** Complete drafted (or reviewed) page content, as the API returns and accepts it. */
export type OnePagerDraft = components["schemas"]["OnePagerDraftV1"];

/** One body module of the page. */
export type ModuleKey = components["schemas"]["OnePagerModuleKeyV1"];

/** One header/footer shell of the master template. */
export type ShellVariant = components["schemas"]["OnePagerShellVariantV1"];

/** The drafting request posted to the backend. */
export type DraftRequest = components["schemas"]["OnePagerDraftRequestV1"];

/** The page-creation request posted to the backend. */
export type CreateRequest = components["schemas"]["CreateOnePagerRequestV1"];

/** State of the caller's Google Drive/Slides connection, shared with the other deck tools. */
export type GoogleStatus = components["schemas"]["StandardConnectionsV1"]["googleStatus"];

/** The three stages of the builder: brief the page, review the draft, show the created page. */
export type BuilderStep = "setup" | "review" | "done";

/** Everything the user enters before drafting. */
export interface BriefState {
    category: string;
    clientName: string;
    guidanceNotes: string;
    shellVariant: ShellVariant;
    /** Stacking order of the active modules, top to bottom; a module is active by being here. */
    moduleOrder: ModuleKey[];
}

/** Text inputs of the brief form that can fail validation. */
export type BriefTextField = "category" | "clientName" | "guidanceNotes";

/** Client-side validation messages keyed by the brief field they belong to. */
export type BriefErrors = Partial<Record<BriefTextField, string>>;

/** One shell offered by the picker. */
export interface ShellOption {
    value: ShellVariant;
    label: string;
    thumbnail: string;
    headerThumbnail: string;
    footerThumbnail: string;
}

/** One body module offered by the picker. */
export interface ModuleOption {
    key: ModuleKey;
    title: string;
    description: string;
    thumbnail: string;
    /** Module that must be active for this one to be pickable. */
    requires?: ModuleKey;
}

/** A module placed on the preview grid, in raw EMU. */
export interface RawPlacement {
    key: ModuleKey;
    leftEmu: number;
    widthEmu: number;
    topEmu: number;
    heightEmu: number;
}

/** A module placed on the canvas, as percentages of the page. */
export interface Placement {
    key: ModuleKey;
    leftPct: number;
    widthPct: number;
    topPct: number;
    heightPct: number;
    /** False when the stack hit the page-height ceiling and this module runs off the page. */
    fits: boolean;
}

/** The computed preview: every placement plus the page height the percentages divide by. */
export interface PreviewLayout {
    placements: Placement[];
    pageHeightEmu: number;
}

/** A row-boundary drop target rendered while a drag is in progress. */
export interface DropGap {
    id: string;
    topPct: number;
}

/** The recommended empty slot beside the solution cards, when the companion visual is not placed. */
export interface SlotBox {
    leftPct: number;
    widthPct: number;
    topPct: number;
    heightPct: number;
}

/** Props of the brief form (setup step). */
export interface BriefFormProps {
    brief: BriefState;
    errors: BriefErrors;
    aiConnected: boolean;
    connectionsLoaded: boolean;
    drafting: boolean;
    draftError: string | null;
    onBriefChange: <K extends BriefTextField>(field: K, value: BriefState[K]) => void;
    onShellChange: (shell: ShellVariant) => void;
    onModuleToggle: (key: ModuleKey, active: boolean) => void;
    onSubmit: () => void;
}

/** Props of a radio or checkbox picker card. */
export interface VariantCardProps {
    name: string;
    kind: "radio" | "checkbox";
    checked: boolean;
    disabled?: boolean;
    label: string;
    description?: string;
    thumbnail: string;
    shell?: boolean;
    onToggle: (checked: boolean) => void;
}

/** Props of the live outline (shell canvas with draggable modules). */
export interface LiveOutlineProps {
    brief: BriefState;
    onReorder: (next: ModuleKey[]) => void;
    onRemove: (key: ModuleKey) => void;
    onAddSolutionsVisual: () => void;
}

/** Props of a module placed on the canvas. */
export interface CanvasBlockProps {
    placement: Placement;
    onRemove: (key: ModuleKey) => void;
}

/** Props of the recommended empty slot beside the solution cards. */
export interface RecommendedSlotProps {
    box: SlotBox;
    rejecting: boolean;
    onAdd: () => void;
}

/** Props of a row-boundary drop gap. */
export interface DropGapProps {
    gap: DropGap;
}

/** Props of a droppable list zone (canvas or overflow tray). */
export interface DropZoneProps {
    id: string;
    className: string;
    items: ModuleKey[];
    children: React.ReactNode;
}

/** Props of a module row in the overflow tray. */
export interface OverflowRowProps {
    moduleKey: ModuleKey;
    onRemove: (key: ModuleKey) => void;
}

/** Props of a module preview image. */
export interface ModulePreviewProps {
    moduleKey: ModuleKey;
}

/** Props of the draft review (review step). */
export interface DraftReviewProps {
    draft: OnePagerDraft;
    googleStatus: GoogleStatus;
    connectionsLoaded: boolean;
    creating: boolean;
    createError: string | null;
    onChange: (next: OnePagerDraft) => void;
    onStartOver: () => void;
    onCreate: () => void;
}

/** Props of a single review text control. */
export interface ReviewFieldProps {
    id: string;
    label: string;
    value: string;
    multiline?: boolean;
    rows?: number;
    onChange: (value: string) => void;
}

/** Props of the done step. */
export interface DoneStepProps {
    slidesUrl: string | null;
    onStartOver: () => void;
}

/** Props of the Google access notice. */
export interface GoogleAccessNoticeProps {
    status: GoogleStatus;
}
