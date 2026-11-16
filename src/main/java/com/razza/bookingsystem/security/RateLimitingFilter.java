package com.razza.bookingsystem.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter that protects the API from too many requests.
 *
 * Each client gets its own bucket, identified by user ID for authenticated requests
 * and by IP address otherwise. When a bucket runs out of tokens, the request is
 * rejected with a 429 TOO MANY REQUESTS response.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    /** One bucket per key (user ID or IP address). */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Core filter logic executed for each HTTP request.
     *
     * Looks up the bucket of the client and tries to consume one token.
     * If it succeeds the request goes on, otherwise a 429 response is returned.
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain)
            throws ServletException, IOException {

        String key = resolveKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(request));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Please slow down.\"}");
        }
    }

    /**
     * Resolves the key used to identify the client.
     * For authenticated requests it is the user ID.
     * For unauthenticated requests (like /auth/**) it is the IP address.
     *
     * @param request the incoming HTTP request
     * @return the key of the client
     */
    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails user) {
            return "user:" + user.getId().toString();
        }
        return "ip:" + getClientIp(request);
    }

    /**
     * Creates a bucket with different limits for auth and normal endpoints.
     * Auth endpoints get stricter limits to protect from brute force attacks.
     *
     * @param request the incoming HTTP request
     * @return a new bucket with the right limit
     */
    private Bucket createBucket(HttpServletRequest request) {
        boolean isAuthEndpoint = request.getRequestURI().startsWith("/auth/");

        Bandwidth limit = isAuthEndpoint
                // Auth: 10 requests per minute (strict, for brute force protection)
                ? Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)))
                // Authenticated users: 100 requests per minute
                : Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));

        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Gets the IP address of the client.
     * It uses the first address of the X-Forwarded-For header if present,
     * otherwise the remote address of the request.
     *
     * @param request the incoming HTTP request
     * @return the IP address of the client
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}