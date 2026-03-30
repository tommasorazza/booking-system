package com.razza.bookingsystem.security;

import com.razza.bookingsystem.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter that processes incoming HTTP requests
 * and sets authentication in the Spring Security context.
 *
 * This filter:
 * - Extracts the JWT token from the Authorization header
 * - Validates the token
 * - Loads the corresponding user from the database
 * - Extracts roles from the token and converts them into authorities
 * - Sets the authenticated user in the SecurityContext
 *
 * It runs once per request and ensures that protected endpoints
 * are accessed only by authenticated users.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Service responsible for JWT operations such as parsing and validation.
     */
    private final JwtService jwtService;

    /**
     * Service used to load user details from the database.
     */
    private final CustomUserDetailsService userDetailsService;

    /**
     * Core filter logic executed for each HTTP request.
     *
     * Steps:
     * 1. Read the Authorization header
     * 2. Check if it contains a Bearer token
     * 3. Extract email (username) from the token
     * 4. Load user details
     * 5. Validate the token
     * 6. Extract roles and convert to Spring Security authorities
     * 7. Create an authentication token and store it in the SecurityContext
     *
     * If no valid token is found, the request proceeds without authentication.
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        /**
         * Extract Authorization header from the request.
         */
        final String authHeader = request.getHeader("Authorization");

        /**
         * If header is missing or does not contain a Bearer token,
         * continue the filter chain without setting authentication.
         */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        /**
         * Extract JWT token and email from it.
         */
        final String jwt = authHeader.substring(7);
        final String email = jwtService.extractEmail(jwt);

        /**
         * Proceed only if email is present and no authentication is set yet.
         */
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            /**
             * Load user details from the database.
             */
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            /**
             * Validate the JWT token against user details.
             */
            if (jwtService.isTokenValid(jwt, userDetails)) {

                /**
                 * Extract roles from the token and convert them into authorities.
                 * Each role is prefixed with ROLE_ as required by Spring Security.
                 */
                List<String> roles = jwtService.extractRoles(jwt);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                /**
                 * Create authentication token with user details and authorities.
                 */
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities
                        );

                /**
                 * Attach request-specific details to the authentication object.
                 */
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                /**
                 * Store authentication in the SecurityContext.
                 */
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        /**
         * Continue processing the request.
         */
        filterChain.doFilter(request, response);
    }
}