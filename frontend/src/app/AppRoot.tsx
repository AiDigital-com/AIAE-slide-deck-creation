import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { QueryClientProvider } from "@tanstack/react-query";

import App from "../App";
import { CaseStudyBuilderPage } from "../features/case-study-builder/ui/CaseStudyBuilderPage";
import { CategoryAnalysisBuilderPage } from "../features/category-analysis-builder/ui/CategoryAnalysisBuilderPage";
import { RfpOutlineGeneratorPage } from "../features/rfp-outline-generator/ui/RfpOutlineGeneratorPage";
import { RndRequestTriagePage } from "../features/rnd-request-triage/ui/RndRequestTriagePage";
import Login from "../pages/Login";
import { AuthProvider } from "../shared/auth/AuthProvider";
import { ProtectedRoute } from "../shared/auth/ProtectedRoute";
import { queryClient } from "./constants/query-client";
import { RND_REQUEST_TRIAGE_ROUTE, isRndRequestTriageAvailable } from "./constants/routes";

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
                                        <Route path="/case-study-builder" element={<CaseStudyBuilderPage />} />
                                        <Route path="/category-analysis-builder" element={<CategoryAnalysisBuilderPage />} />
                                        <Route path="/rfp-outline-generator" element={<RfpOutlineGeneratorPage />} />
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
