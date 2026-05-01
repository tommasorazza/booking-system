package com.razza.bookingsystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razza.bookingsystem.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for rate limiting on the authentication endpoint.
 *
 * The full Spring context is started for these tests, including the security
 * filter chain, so requests pass through RateLimitingFilter exactly as they
 * would in production. Rate limiting on auth endpoints is IP-based, since
 * no JWT exists yet when a user is trying to log in.
 *
 * MockMvc sends all requests from 127.0.0.1, so all requests in a test
 * share the same IP bucket.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RateLimitingAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Verifies that the login endpoint blocks a client with HTTP 429
     * after 10 attempts from the same IP, regardless of credentials.
     *
     * Wrong credentials are used on purpose so the first 10 requests
     * return 401 Unauthorized. This proves the rate limiter counts
     * every request, not just successful ones — exactly as it would
     * against a real attack. The 11th request must be
     * blocked by the filter before authentication logic even runs.
     */
    @Test
    void loginEndpoint_shouldReturn429AfterTenAttempts() throws Exception {
        LoginRequest body = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("wrongpassword")
                .tenantName("Tenant A")
                .build();

        String json = objectMapper.writeValueAsString(body);

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests());
    }
}