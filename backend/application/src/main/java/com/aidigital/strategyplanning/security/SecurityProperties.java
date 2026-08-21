package com.aidigital.strategyplanning.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed browser security configuration bound from {@code app.security.*}.
 */
@Getter
@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

	private final Cors cors = new Cors();
	private final Csp csp = new Csp();

	/**
	 * Cross-origin resource sharing settings.
	 */
	@Getter
	@Setter
	public static class Cors {

		@NotBlank
		private String allowedOrigins =
				"https://*.replit.dev,https://*.repl.co,http://localhost:5173,http://localhost:5000";

		private long maxAgeSeconds = 3600L;
	}

	/**
	 * Content Security Policy settings.
	 *
	 * <p>Clerk's own Frontend API origin is not listed here: {@code SecurityConfig} appends the
	 * exact host decoded from the publishable key to {@code script-src} and {@code frame-src}, so
	 * trust is pinned to this instance instead of the whole {@code *.clerk.accounts.dev} wildcard.
	 */
	@Getter
	@Setter
	public static class Csp {

		@NotBlank
		private String defaultSrc = "'self'";

		@NotBlank
		private String frameAncestors = "'self' https://*.replit.dev https://*.repl.co";

		@NotBlank
		private String scriptSrc = "'self' 'unsafe-inline' https://challenges.cloudflare.com";

		@NotBlank
		private String workerSrc = "'self' blob:";

		@NotBlank
		private String frameSrc = "'self' https://challenges.cloudflare.com";

		@NotBlank
		private String styleSrc = "'self' 'unsafe-inline' https://fonts.googleapis.com";

		@NotBlank
		private String imgSrc = "'self' data: https:";

		@NotBlank
		private String connectSrc = "'self' https:";

		@NotBlank
		private String fontSrc = "'self' data: https://fonts.gstatic.com";
	}
}
