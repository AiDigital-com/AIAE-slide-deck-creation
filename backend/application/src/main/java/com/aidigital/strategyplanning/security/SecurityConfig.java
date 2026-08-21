package com.aidigital.strategyplanning.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Configures stateless Clerk-SSO API security, the JWT decoder, CORS, and
 * browser security headers.
 */
@Configuration
@EnableConfigurationProperties({AuthProperties.class, SecurityProperties.class})
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final ClerkJwtClaimsValidator clerkJwtClaimsValidator;
    private final ClerkPublishableKeyDecoder publishableKeyDecoder;
    private final CompanyEmailDomainAuthorizationManager companyEmailDomainAuthorizationManager;

    public SecurityConfig(
        SecurityProperties securityProperties,
        ClerkJwtClaimsValidator clerkJwtClaimsValidator,
        ClerkPublishableKeyDecoder publishableKeyDecoder,
        CompanyEmailDomainAuthorizationManager companyEmailDomainAuthorizationManager
    ) {
        this.securityProperties = securityProperties;
        this.clerkJwtClaimsValidator = clerkJwtClaimsValidator;
        this.publishableKeyDecoder = publishableKeyDecoder;
        this.companyEmailDomainAuthorizationManager = companyEmailDomainAuthorizationManager;
    }

    /**
     * Builds the single stateless security chain for API and actuator endpoints.
     *
     * @param http Spring Security HTTP builder
     * @param authProperties auth configuration bound from {@code app.auth.*}
     * @return configured security filter chain
     * @throws Exception propagated by Spring Security when the chain cannot be built
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthProperties authProperties) throws Exception {
        String clerkOrigin = clerkCspOrigin(authProperties);
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h
                .frameOptions(frame -> frame.disable())
                .contentSecurityPolicy(csp -> csp.policyDirectives(cspPolicyDirectives(clerkOrigin)))
                .referrerPolicy(r -> r.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentTypeOptions(opts -> { })
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000L)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(AuthConstants.PUBLIC_PATHS).permitAll()
                .anyRequest().access(companyEmailDomainAuthorizationManager))
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                .jwtAuthenticationConverter(userIdAsPrincipalConverter())))
            .build();
    }

    /**
     * Binds {@code Authentication#getName()} to the stable Clerk
     * {@code user_id} claim (not the email).
     *
     * @return converter that pins the principal name to the user_id claim
     */
    JwtAuthenticationConverter userIdAsPrincipalConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName(AuthConstants.USER_ID_CLAIM);
        return converter;
    }

    /**
     * Renders the CSP header from {@code app.security.csp.*}, appending this tenant's Clerk
     * Frontend API origin to the two directives that load Clerk's own code and iframes.
     *
     * @param clerkOrigin Clerk origin token from {@link #clerkCspOrigin}, or an empty string
     * @return the assembled Content-Security-Policy directive list
     */
    String cspPolicyDirectives(String clerkOrigin) {
        SecurityProperties.Csp csp = securityProperties.getCsp();
        return String.join(" ",
            "default-src " + csp.getDefaultSrc() + ";",
            "frame-ancestors " + csp.getFrameAncestors() + ";",
            "script-src " + csp.getScriptSrc() + clerkOrigin + ";",
            "worker-src " + csp.getWorkerSrc() + ";",
            "frame-src " + csp.getFrameSrc() + clerkOrigin + ";",
            "style-src " + csp.getStyleSrc() + ";",
            "img-src " + csp.getImgSrc() + ";",
            "connect-src " + csp.getConnectSrc() + ";",
            "font-src " + csp.getFontSrc() + ";");
    }

    /**
     * Resolves this tenant's exact Clerk Frontend API origin for the CSP from
     * the publishable key, so {@code script-src}/{@code frame-src} trust is
     * pinned to this instance's host rather than the whole
     * {@code *.clerk.accounts.dev} wildcard. Returns an empty string (no Clerk
     * origin added) when no publishable key is configured.
     *
     * @param authProperties auth configuration bound from {@code app.auth.*}
     * @return CSP origin token prefixed with a leading space, or empty string
     */
    String clerkCspOrigin(AuthProperties authProperties) {
        String host = publishableKeyDecoder.decodeFrontendApiHost(authProperties.getPublishableKey());
        return (host == null || host.isBlank()) ? "" : " https://" + host;
    }

    /**
     * CORS source — reads the allow-list from {@code app.security.cors.allowed-origins}.
     *
     * @return URL-pattern CORS configuration applied to all paths
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(Arrays.stream(
                securityProperties.getCors().getAllowedOrigins().split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id", "Accept"));
        cfg.setExposedHeaders(List.of("X-Correlation-Id"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(securityProperties.getCors().getMaxAgeSeconds());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * The Clerk-SSO {@link JwtDecoder}.
     *
     * @param props auth configuration bound from {@code app.auth.*}
     * @return decoder configured for Clerk (or any OIDC issuer)
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(AuthProperties props) {
        return buildSsoDecoder(props);
    }

    /**
     * Builds a Nimbus JwtDecoder with explicit validation.
     *
     * <p>Fails fast when neither a Clerk publishable key nor explicit issuer/JWKS URIs are
     * configured: SSO is the only auth mode, so a silently unauthenticated boot is not a valid
     * state. Startup aborts rather than serving an app whose API rejects every caller.
     *
     * @param props auth configuration bound from {@code app.auth.*}
     * @return decoder configured for Clerk (or any OIDC issuer)
     */
    JwtDecoder buildSsoDecoder(AuthProperties props) {
        resolveSsoEndpoints(props);

        String issuer = props.getSso().getIssuerUri();
        String jwkSetUri = props.getSso().getJwkSetUri();
        String audience = props.getSso().getAudience();

        if ((issuer == null || issuer.isBlank()) && (jwkSetUri == null || jwkSetUri.isBlank())) {
            throw new IllegalStateException(
                "Clerk SSO is required but unconfigured: set CLERK_PUBLISHABLE_KEY or "
                + "AUTH_ISSUER_URI / AUTH_JWKS_URI. This template has no mock fallback.");
        }

        NimbusJwtDecoder decoder = (jwkSetUri != null && !jwkSetUri.isBlank())
            ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
            : (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> defaultValidator = (issuer != null && !issuer.isBlank())
            ? JwtValidators.createDefaultWithIssuer(issuer)
            : JwtValidators.createDefault();

        OAuth2TokenValidator<Jwt> audienceValidator = null;
        if (audience != null && !audience.isBlank()) {
            audienceValidator = new JwtClaimValidator<>(
                JwtClaimNames.AUD,
                claim -> claim instanceof Collection<?> c
                    ? c.contains(audience)
                    : audience.equals(claim));
        }

        OAuth2TokenValidator<Jwt> composite = audienceValidator == null
            ? new DelegatingOAuth2TokenValidator<>(
                defaultValidator, clerkJwtClaimsValidator)
            : new DelegatingOAuth2TokenValidator<>(
                defaultValidator, audienceValidator, clerkJwtClaimsValidator);

        decoder.setJwtValidator(composite);
        return decoder;
    }

    /**
     * Fills issuer and JWKS from {@code CLERK_PUBLISHABLE_KEY} when explicit URIs are blank.
     *
     * @param props bound auth configuration
     */
    private void resolveSsoEndpoints(AuthProperties props) {
        AuthProperties.Sso sso = props.getSso();
        if ((sso.getIssuerUri() == null || sso.getIssuerUri().isBlank())
            && (sso.getJwkSetUri() == null || sso.getJwkSetUri().isBlank())
            && props.getPublishableKey() != null && !props.getPublishableKey().isBlank()) {
            String issuer = publishableKeyDecoder.issuerFromPublishableKey(props.getPublishableKey());
            sso.setIssuerUri(issuer);
            sso.setJwkSetUri(publishableKeyDecoder.jwksUriFromPublishableKey(props.getPublishableKey()));
        }
    }
}
