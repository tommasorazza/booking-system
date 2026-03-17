package com.razza.bookingsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter that intercepts every HTTP request to perform JWT-based authentication.
 *
 * This filter extracts the JWT token from the "Authorization" header,
 * validates it, and, if valid, populates the Spring Security context
 * with the authenticated user's information.
 *
 * It runs once per request because it extends
 * {@link OncePerRequestFilter}.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Service responsible for generating and validating JWT tokens.
     */
    private final JwtService jwtService;

    /**
     * Intercepts the HTTP request, extracts and validates the JWT token if present,
     * and sets the authentication in the security context.
     *
     * If no token is present, the request proceeds without authentication.
     * After authentication (or if no token is found), the filter passes control
     * to the next filter in the chain.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain for continuing request processing
     * @throws ServletException if an internal servlet error occurs
     * @throws IOException      if an I/O error occurs during request processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Read the Authorization header
        String authHeader = request.getHeader("Authorization");

        // If header is missing or doesn't start with "Bearer", continue the chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the token by removing "Bearer " prefix
        String token = authHeader.substring(7);

        // Extract email (user identity) from the JWT
        String email = jwtService.extractEmail(token);

        // If token is valid, create an Authentication object
        if (email != null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,                  // principal (user identity)
                            null,                   // credentials (not needed)
                            Collections.emptyList() // authorities (roles)
                    );

            // Store authentication in Spring Security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}