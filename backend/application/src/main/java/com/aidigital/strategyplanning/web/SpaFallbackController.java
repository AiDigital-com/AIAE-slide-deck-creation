// SpaFallbackController — serves index.html for client-side React Router paths
// when Spring Boot hosts the built SPA on port 5000 (Replit Deployment).
// Without this, deep links and browser refresh on non-root routes return 404.

package com.aidigital.strategyplanning.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards unmatched SPA routes to the React entry point.
 */
@Controller
public class SpaFallbackController {

	private static final Resource INDEX = new ClassPathResource("static/index.html");

	/**
	 * Top-level path prefixes the server owns, so they are never answered with the React shell:
	 * the API, operational endpoints, hashed build assets, generated images, and the folders Vite
	 * copies from {@code frontend/public} (tool icons, Case Study icons that Google Slides fetches,
	 * and the In-Depth and One-Pager preview images). Add a prefix here whenever a new public
	 * folder is introduced, together with its entry in {@code AuthConstants.PUBLIC_PATHS}.
	 */
	private static final String SERVER_OWNED_PREFIXES = "api|actuator|swagger-ui|v3|assets|error|public"
			+ "|favicon\\.ico|tool-icons|case-study-icons|in-depth-slides|one-pager-modules";

	/**
	 * Root and explicit index requests.
	 *
	 * @return the SPA shell when the static build exists
	 */
	@GetMapping(value = {"/", "/index.html"})
	public ResponseEntity<Resource> root() {
		return indexHtml();
	}

	/**
	 * Single-segment client routes (e.g. {@code /login}, {@code /reports}).
	 *
	 * @return the SPA shell when the path is not an API/static asset
	 */
	@GetMapping("/{path:^(?!" + SERVER_OWNED_PREFIXES + ")[^\\.]+$}")
	public ResponseEntity<Resource> spaSingleSegment() {
		return indexHtml();
	}

	/**
	 * Nested client routes (e.g. {@code /reports/123/edit}).
	 *
	 * @return the SPA shell when the path is not an API/static asset
	 */
	@GetMapping("/{path:^(?!" + SERVER_OWNED_PREFIXES + ")[^\\.]+}/**")
	public ResponseEntity<Resource> spaNested() {
		return indexHtml();
	}

	/**
	 * Returns the built SPA shell, or 404 when the frontend has not been packaged.
	 *
	 * @return HTML response for client-side routing, or not found
	 */
	private ResponseEntity<Resource> indexHtml() {
		if (!INDEX.exists()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(INDEX);
	}
}
