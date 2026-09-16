package com.jpmorgan.midascore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * ApiKeyAuthFilter — Custom authentication filter for API key validation (UP-3).
 *
 * This filter intercepts all HTTP requests and validates the X-API-Key header
 * before allowing the request to proceed. It provides a simple, stateless
 * authentication mechanism suitable for service-to-service communication.
 *
 * Design Pattern: Header-based API key authentication
 *   - Stateless (no session required)
 *   - Non-intrusive (only checks one header)
 *   - Suitable for microservices
 *   - Externalized configuration (not hard-coded)
 *
 * Flow:
 *   1. Intercept request via OncePerRequestFilter
 *   2. Check for X-API-Key header
 *   3. Compare with configured valid key
 *   4. If valid: allow request to proceed
 *   5. If invalid/missing: return 401 Unauthorized
 *
 * Used By: SecurityConfig.java (registered in filter chain)
 *
 * Configuration: application.yml
 *   security:
 *     api-key: ${MIDAS_API_KEY:midas-dev-key-2026}
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${security.api-key}")
    private String validApiKey;

    /**
     * Filter method invoked once per request.
     * Validates X-API-Key header and either allows or denies access.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
                                    throws ServletException, IOException {

        // Extract API key from request header
        String apiKey = request.getHeader(API_KEY_HEADER);

        // Log authentication attempt
        log.debug("Authentication attempt | path={} | apiKeyPresent={}", 
            request.getRequestURI(), apiKey != null);

        // Validate API key
        if (validApiKey != null && validApiKey.equals(apiKey)) {
            // Valid API key — allow request to proceed
            log.debug("Authentication successful | path={}", request.getRequestURI());
            filterChain.doFilter(request, response);
        } else {
            // Invalid or missing API key — return 401 Unauthorized
            log.warn("Authentication failed | path={} | reason=INVALID_OR_MISSING_API_KEY", 
                request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized — invalid or missing API key\"}");
        }
    }

    /**
     * Override to apply filter only to protected endpoints.
     * By default, filters all requests. SecurityConfig can customize which paths to filter.
     *
     * @param request the HTTP request
     * @return true if filter should be applied, false otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Filter all requests by default
        // Specific path exclusions can be added here if needed
        return false;
    }
}
