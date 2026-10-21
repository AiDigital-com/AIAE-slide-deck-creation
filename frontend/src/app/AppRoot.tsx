import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { QueryClientProvider } from "@tanstack/react-query";

import App from "../App";
import { CaseStudyBuilderPage } from "../features/case-study-builder/ui/CaseStudyBuilderPage";
import { CaseStudyTemplatePickerPage } from "../features/case-study-builder/ui/CaseStudyTemplatePickerPage";
import { OnePageCaseStudyPage } from "../features/case-study-builder/ui/OnePageCaseStudyPage";
import { CategoryAnalysisBuilderPage } from "../features/category-analysis-builder/ui/CategoryAnalysisBuilderPage";
import { InDepthCategoryAnalysisBuilderPage } from "../features/category-analysis-builder/ui/InDepthCategoryAnalysisBuilderPage";
import { TemplatePickerPage } from "../features/category-analysis-builder/ui/TemplatePickerPage";
import { OnePageGeneratorPage } from "../features/one-page-generator/ui/OnePageGeneratorPage";
import { RfpOutlineGeneratorPage } from "../features/rfp-outline-generator/ui/RfpOutlineGeneratorPage";
import { RndRequestTriagePage } from "../features/rnd-request-triage/ui/RndRequestTriagePage";
import Login from "../pages/Login";
import { AuthProvider } from "../shared/auth/AuthProvider";
import { ProtectedRoute } from "../shared/auth/ProtectedRoute";
import { queryClient } from "./constants/query-client";
import {
    CASE_STUDY_ROUTES,
    CATEGORY_ANALYSIS_ROUTES,
    ONE_PAGER_ROUTE,
    RND_REQUEST_TRIAGE_ROUTE,
    isRndRequestTriageAvailable,
} from "./constants/routes";

/** Router + providers — main.tsx mounts only this component. */
export function AppRoot() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <QueryClientProvider client={queryClient}>
                    <Routes>
                        <Route path="/login" element={<Login />} />
                        <Route
                            path="/*"
                            element={
                                <ProtectedRoute>
                                    <Routes>
                                        <Route path="/" element={<App />} />
                                        <Route path={CASE_STUDY_ROUTES.picker} element={<CaseStudyTemplatePickerPage />} />
                                        <Route path={CASE_STUDY_ROUTES.threePage} element={<CaseStudyBuilderPage />} />
                                        <Route path={CASE_STUDY_ROUTES.onePage} element={<OnePageCaseStudyPage />} />
                                        <Route path={CATEGORY_ANALYSIS_ROUTES.picker} element={<TemplatePickerPage />} />
                                        <Route path={CATEGORY_ANALYSIS_ROUTES.standard} element={<CategoryAnalysisBuilderPage />} />
                                        <Route path={CATEGORY_ANALYSIS_ROUTES.inDepth} element={<InDepthCategoryAnalysisBuilderPage />} />
                                        <Route path="/rfp-outline-generator" element={<RfpOutlineGeneratorPage />} />
                                        <Route path={ONE_PAGER_ROUTE} element={<OnePageGeneratorPage />} />
                                        <Route
                                            path={RND_REQUEST_TRIAGE_ROUTE}
                                            element={isRndRequestTriageAvailable ? <RndRequestTriagePage /> : <Navigate to="/" replace />}
                                        />
                                    </Routes>
                                </ProtectedRoute>
                            }
                        />
                    </Routes>
                </QueryClientProvider>
            </AuthProvider>
        </BrowserRouter>
    );
}
