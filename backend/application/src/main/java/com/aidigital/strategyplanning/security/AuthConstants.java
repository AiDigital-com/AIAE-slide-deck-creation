// Shared auth constants. SSO-only: the single concern here is which routes
// stay public (the SPA shell, static assets, health/metrics, the OpenAPI
// surface). Everything else requires a valid Clerk Bearer JWT.

package com.aidigital.strategyplanning.security;

/**
 * Shared constants for public routes.
 */
public final class AuthConstants {

	private AuthConstants() {
	}

	/**
	 * Clerk JWT claim bound as {@code Authentication#getName()}.
	 */
	public static final String USER_ID_CLAIM = "user_id";

	/**
	 * Path patterns that must remain public (no Bearer JWT required).
	 */
	public static final String[] PUBLIC_PATHS = {
			"/",
			"/index.html",
			"/favicon.ico",
			"/assets/**",
			"/error",
			"/*.css",
			"/*.js",
			"/*.png",
			"/*.svg",
			"/login",
			"/login/**",
			"/sign-in",
			"/sign-in/**",
			"/sign-up",
			"/sign-up/**",
			// SPA client-side routes — a browser navigation carries no Bearer header, so the
			// shell must be reachable and the React ProtectedRoute gates it. Every route in
			// app/AppRoot.tsx belongs here; a missing one returns 401 on refresh/deep link.
			"/case-study-builder",
			"/case-study-builder/**",
			"/category-analysis-builder",
			"/category-analysis-builder/**",
			"/rfp-outline-generator",
			"/rfp-outline-generator/**",
			"/rnd-request-triage",
			"/rnd-request-triage/**",
			// Short-lived generated slide images fetched by Google Slides (unauthenticated by design)
			"/public/slide-images/**",
			"/actuator/health",
			"/actuator/prometheus",
			"/api/v1/specs/**",
			"/swagger-ui/**",
			"/v3/api-docs/**"
	};
}
