package com.creatorengine.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless security setup.
 *
 * <ul>
 *   <li>Sessions disabled — every request carries a JWT.</li>
 *   <li>CSRF disabled — irrelevant for token-based APIs called by SPAs.</li>
 *   <li>{@link JwtAuthenticationFilter} runs before
 *       {@link UsernamePasswordAuthenticationFilter} so authenticated
 *       requests bypass the form-login pipeline entirely.</li>
 *   <li>{@code /api/auth/**}, health probes, and Swagger are public;
 *       everything else requires authentication.</li>
 *   <li>{@code @EnableMethodSecurity} enables {@code @PreAuthorize} for
 *       fine-grained role checks at the controller level.</li>
 * </ul>
 *
 * <p>The {@link PasswordEncoder} bean is exposed for completeness even
 * though Firebase Auth performs the actual password hashing — it's used
 * for any internal secrets we might need to hash (e.g. API keys) later.</p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authEntryPoint;
    private final UrlBasedCorsConfigurationSource corsConfigurationSource;

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/refresh",
            // Meta hits these directly — no JWT possible from their side.
            // The OAuth callback authenticates via the signed `state` token,
            // and the webhook authenticates via X-Hub-Signature-256.
            // NOTE: only the exact /api/webhook path is public — dev test
            // endpoints under /api/webhook/test still require authentication.
            "/api/instagram/callback",
            "/api/webhook",
            "/api/webhooks/**",
            "/api/health",
            "/actuator/health",
            "/actuator/info",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(authEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
