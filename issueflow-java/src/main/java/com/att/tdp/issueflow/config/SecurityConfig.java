package com.att.tdp.issueflow.config;

import com.att.tdp.issueflow.security.JwtAuthenticationFilter;
import com.att.tdp.issueflow.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.Map;

/**
 * Central Spring Security configuration.
 *
 * <p>Key design decisions:</p>
 * <ul>
 *   <li><b>Stateless</b> – no HTTP session; every request must carry a JWT.</li>
 *   <li><b>CSRF disabled</b> – safe for stateless REST APIs (no cookie-based auth).</li>
 *   <li><b>{@code POST /auth/login} open</b> – all other endpoints require a valid JWT.</li>
 *   <li><b>{@code @EnableMethodSecurity}</b> – enables {@code @PreAuthorize} for
 *       fine-grained ADMIN/DEVELOPER role checks on individual endpoints (Phases 4–12).</li>
 *   <li><b>Custom 401/403 handlers</b> – return structured JSON instead of HTML redirects.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // activates @PreAuthorize / @Secured on controllers & services
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl  userDetailsService;
    private final ObjectMapper            objectMapper;

    // -----------------------------------------------------------------------
    // Security filter chain
    // -----------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Session & CSRF ──────────────────────────────────────────────
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── URL-level access rules ───────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                    // Public: only the login endpoint
                    .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    // Everything else requires authentication
                    // Fine-grained ADMIN/DEVELOPER rules are enforced via @PreAuthorize
                    .anyRequest().authenticated()
            )

            // ── User details ─────────────────────────────────────────────────
            .userDetailsService(userDetailsService)

            // ── JWT filter ───────────────────────────────────────────────────
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ── Custom 401 / 403 JSON responses ─────────────────────────────
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) ->
                            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                    "UNAUTHORIZED", "Authentication required — provide a valid Bearer token"))
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                    "FORBIDDEN", "You do not have permission to perform this action"))
            );

        return http.build();
    }

    // -----------------------------------------------------------------------
    // Shared beans
    // -----------------------------------------------------------------------

    /**
     * BCrypt password encoder used throughout the application.
     * Injected by {@code AuthService} for login credential verification
     * and by {@code UserService} for hashing passwords on user creation.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the {@link AuthenticationManager} as a bean so that
     * {@code AuthService} can delegate credential authentication to Spring Security.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /** Writes a structured JSON error body directly to the servlet response. */
    private void writeError(HttpServletResponse response, int status,
                            String error, String message) {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            response.getWriter().write(
                    objectMapper.writeValueAsString(Map.of(
                            "error",     error,
                            "message",   message,
                            "timestamp", Instant.now().toString()
                    ))
            );
        } catch (Exception ignored) {
            // Nothing useful we can do here
        }
    }
}
