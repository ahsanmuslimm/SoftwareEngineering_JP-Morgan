package com.jpmorgan.midascore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — Spring Security configuration for API authentication (UP-3).
 *
 * This configuration sets up stateless API authentication using custom API key filter.
 * It is designed for service-to-service communication and REST API security.
 *
 * Security Configuration:
 *   - Stateless session management (no HttpSession)
 *   - CSRF disabled (appropriate for stateless APIs)
 *   - Custom API key filter (X-API-Key header validation)
 *   - Requires authentication for all requests
 *
 * Design Pattern: Filter-based stateless authentication
 *   - No session storage required
 *   - Simple API key validation
 *   - Suitable for microservices
 *   - Easy to scale horizontally
 *
 * Authentication Flow:
 *   1. Request arrives at /balance endpoint
 *   2. ApiKeyAuthFilter intercepts request
 *   3. X-API-Key header is validated
 *   4. If valid: request proceeds to controller
 *   5. If invalid: 401 Unauthorized response returned
 *
 * Configuration: application.yml
 *   security:
 *     api-key: ${MIDAS_API_KEY:midas-dev-key-2026}
 *
 * Used By: Spring Security auto-configuration
 * Depends On: ApiKeyAuthFilter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security filter chain configuration.
     * Sets up HTTP security with stateless API authentication.
     *
     * @param http the HttpSecurity builder
     * @param apiKeyAuthFilter the custom API key authentication filter
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration error occurs
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ApiKeyAuthFilter apiKeyAuthFilter)
            throws Exception {

        http
            // ─── CSRF Protection ────────────────────────────────────────
            // Disabled for stateless API (no session = no CSRF token needed)
            .csrf(csrf -> csrf.disable())

            // ─── Session Management ─────────────────────────────────────
            // Stateless: Each request must authenticate independently
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ─── Register Custom Filter ─────────────────────────────────
            // Add API key filter before username/password filter
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ─── Authorization Rules ────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // All requests require authentication (handled by filter)
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
