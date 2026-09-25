// @ConfigurationProperties bean — typed home for Clerk SSO settings.
// Maps from application.yml `app.auth.*` and the AUTH_* / CLERK_* env vars.
// Accessors are written out on purpose: Lombok is avoided in Spring framework
// glue, where an annotation-processing failure surfaces as a misleading
// "cannot find symbol getSso()" instead of the real cause.

package com.aidigital.strategyplanning.security;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for Clerk SSO authentication.
 */
@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

	/**
	 * Company email domain without a leading {@code @}.
	 */
	private String allowedEmailDomain = "aidigital.com";

	/**
	 * Clerk publishable key — derives issuer/JWKS when explicit URIs are blank.
	 */
	private String publishableKey;

	/**
	 * Comma-separated list of exact trusted browser origins for the JWT
	 * {@code azp} (authorized party) claim, e.g.
	 * {@code http://localhost:5173,https://my-app.replit.app}. Never a Clerk
	 * publishable key. Blank fails startup via {@link AuthStartupValidator}.
	 */
	private String authorizedParties = "";

	@NotNull
	private Sso sso = new Sso();

	/**
	 * Returns the company email domain allowed to sign in.
	 *
	 * @return email domain without a leading {@code @}
	 */
	public String getAllowedEmailDomain() {
		return allowedEmailDomain;
	}

	/**
	 * Sets the company email domain allowed to sign in.
	 *
	 * @param allowedEmailDomain email domain without a leading {@code @}
	 */
	public void setAllowedEmailDomain(String allowedEmailDomain) {
		this.allowedEmailDomain = allowedEmailDomain;
	}

	/**
	 * Returns the Clerk publishable key.
	 *
	 * @return publishable key, or null when issuer/JWKS are configured explicitly
	 */
	public String getPublishableKey() {
		return publishableKey;
	}

	/**
	 * Sets the Clerk publishable key.
	 *
	 * @param publishableKey publishable key to derive issuer/JWKS from
	 */
	public void setPublishableKey(String publishableKey) {
		this.publishableKey = publishableKey;
	}

	/**
	 * Returns the trusted browser origins accepted in the JWT {@code azp} claim.
	 *
	 * @return comma-separated origin list, blank when unset
	 */
	public String getAuthorizedParties() {
		return authorizedParties;
	}

	/**
	 * Sets the trusted browser origins accepted in the JWT {@code azp} claim.
	 *
	 * @param authorizedParties comma-separated list of exact origins
	 */
	public void setAuthorizedParties(String authorizedParties) {
		this.authorizedParties = authorizedParties;
	}

	/**
	 * Returns the SSO/OIDC sub-group.
	 *
	 * @return SSO settings, never null
	 */
	public Sso getSso() {
		return sso;
	}

	/**
	 * Sets the SSO/OIDC sub-group.
	 *
	 * @param sso SSO settings to bind
	 */
	public void setSso(Sso sso) {
		this.sso = sso;
	}

	/**
	 * SSO/OIDC settings for Clerk or another compatible provider.
	 */
	public static class Sso {

		/**
		 * Clerk issuer URI, e.g. https://clean-clerk.clerk.accounts.dev
		 */
		private String issuerUri;

		/**
		 * Optional JWKS override; usually discovered from issuerUri.
		 */
		private String jwkSetUri;

		/**
		 * Expected `aud` claim value.
		 */
		private String audience;

		/**
		 * Returns the issuer URI.
		 *
		 * @return issuer URI, or null when derived from the publishable key
		 */
		public String getIssuerUri() {
			return issuerUri;
		}

		/**
		 * Sets the issuer URI.
		 *
		 * @param issuerUri issuer URI to use
		 */
		public void setIssuerUri(String issuerUri) {
			this.issuerUri = issuerUri;
		}

		/**
		 * Returns the JWKS URI override.
		 *
		 * @return JWKS URI, or null when discovered from the issuer
		 */
		public String getJwkSetUri() {
			return jwkSetUri;
		}

		/**
		 * Sets the JWKS URI override.
		 *
		 * @param jwkSetUri JWKS URI to use instead of discovery
		 */
		public void setJwkSetUri(String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		/**
		 * Returns the expected audience claim.
		 *
		 * @return expected {@code aud} value, or null when unchecked
		 */
		public String getAudience() {
			return audience;
		}

		/**
		 * Sets the expected audience claim.
		 *
		 * @param audience expected {@code aud} value
		 */
		public void setAudience(String audience) {
			this.audience = audience;
		}
	}
}
