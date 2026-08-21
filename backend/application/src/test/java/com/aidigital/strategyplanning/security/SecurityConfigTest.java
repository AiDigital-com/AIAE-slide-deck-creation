package com.aidigital.strategyplanning.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the Clerk-SSO {@link JwtDecoder} wiring and the rendered CSP header.
 */
class SecurityConfigTest {

	private final SecurityProperties securityProperties = new SecurityProperties();
	private final ClerkJwtClaimsValidator clerkJwtClaimsValidator =
			new ClerkJwtClaimsValidator(new AuthProperties());
	private final ClerkPublishableKeyDecoder publishableKeyDecoder =
			new ClerkPublishableKeyDecoder();
	private final CompanyEmailDomainAuthorizationManager companyEmailDomainAuthorizationManager =
			new CompanyEmailDomainAuthorizationManager(new AuthProperties());
	private final SecurityConfig securityConfig = new SecurityConfig(
			securityProperties, clerkJwtClaimsValidator, publishableKeyDecoder,
			companyEmailDomainAuthorizationManager);

	@Test
	void shouldFailFastWhenSsoIsUnconfiguredTest() {
		AuthProperties props = new AuthProperties();
		assertThatThrownBy(() -> securityConfig.buildSsoDecoder(props))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Clerk SSO is required");
	}

	@Test
	void shouldBuildSignatureValidatingJwksDecoderWhenConfiguredTest() {
		AuthProperties props = new AuthProperties();
		props.getSso().setJwkSetUri("https://clerk.example.com/.well-known/jwks.json");
		JwtDecoder decoder = securityConfig.buildSsoDecoder(props);
		assertThat(decoder).isInstanceOf(NimbusJwtDecoder.class);
	}

	@Test
	void shouldDeriveIssuerAndJwksFromPublishableKeyTest() {
		AuthProperties props = new AuthProperties();
		String host = "clerk.example.com";
		props.setPublishableKey("pk_test_"
				+ java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString(host.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		JwtDecoder decoder = securityConfig.buildSsoDecoder(props);
		assertThat(decoder).isInstanceOf(NimbusJwtDecoder.class);
		assertThat(props.getSso().getIssuerUri()).isEqualTo("https://clerk.example.com");
		assertThat(props.getSso().getJwkSetUri())
				.isEqualTo("https://clerk.example.com/.well-known/jwks.json");
	}

	@Test
	void shouldRenderCspFromConfiguredDirectivesTest() {
		// Given: the default app.security.csp.* values and no Clerk origin
		// When: the policy is rendered
		String policy = securityConfig.cspPolicyDirectives("");

		// Then: every directive appears exactly once, in the documented order
		assertThat(policy).isEqualTo(
				"default-src 'self'; "
						+ "frame-ancestors 'self' https://*.replit.dev https://*.repl.co; "
						+ "script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com; "
						+ "worker-src 'self' blob:; "
						+ "frame-src 'self' https://challenges.cloudflare.com; "
						+ "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
						+ "img-src 'self' data: https:; "
						+ "connect-src 'self' https:; "
						+ "font-src 'self' data: https://fonts.gstatic.com;");
	}

	@Test
	void shouldPinClerkOriginIntoScriptAndFrameDirectivesOnlyTest() {
		// Given: a publishable key that decodes to this instance's Clerk host
		AuthProperties props = new AuthProperties();
		String host = "clerk.example.com";
		props.setPublishableKey("pk_test_"
				+ java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString(host.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

		// When: the policy is rendered with that origin
		String policy = securityConfig.cspPolicyDirectives(securityConfig.clerkCspOrigin(props));

		// Then: the exact host is trusted for Clerk's script and iframe, and nowhere else
		assertThat(policy).contains(
				"script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com https://clerk.example.com;",
				"frame-src 'self' https://challenges.cloudflare.com https://clerk.example.com;");
		assertThat(policy).doesNotContain("*.clerk.accounts.dev");
		assertThat(policy.split("https://clerk.example.com;", -1)).hasSize(3);
	}

	@Test
	void shouldOmitTheClerkOriginWhenNoPublishableKeyIsSetTest() {
		// Given: auth properties without a publishable key
		// When-Then: no Clerk origin is contributed to the policy
		assertThat(securityConfig.clerkCspOrigin(new AuthProperties())).isEmpty();
	}
}
