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

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // One bucket map per "key" (userId or IP)
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

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
     * For authenticated requests: use userId as key.
     * For unauthenticated (/auth/**): use IP address.
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
     * Different limits for auth vs normal endpoints.
     * Auth endpoints get stricter limits (brute-force protection).
     */
    private Bucket createBucket(HttpServletRequest request) {
        boolean isAuthEndpoint = request.getRequestURI().startsWith("/auth/");

        Bandwidth limit = isAuthEndpoint
                // Auth: 10 requests per minute (strict — brute-force protection)
                ? Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)))
                // Authenticated users: 100 requests per minute
                : Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));

        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}