package com.razza.bookingsystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razza.bookingsystem.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for rate limiting on authenticated endpoints.
 *
 * The full Spring context is started for these tests, including the security
 * filter chain, so requests pass through RateLimitingFilter exactly as they
 * would in production. Rate limiting on authenticated endpoints is user-based,
 * meaning each user has their own independent bucket keyed by their UUID.
 *
 * A fresh JWT for Alice is obtained before each test via a real login request.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RateLimitingAuthenticatedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    /**
     * Logs in as Alice before each test and stores her JWT.
     *
     * Alice is seeded by DataInitializer with email alice@example.com
     * and password "password" in Tenant A. The login request itself
     * consumes one token from the IP bucket for the auth endpoint,
     * but Alice's user bucket for authenticated endpoints starts full.
     */
    @BeforeEach
    void obtainToken() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("alice@example.com")
                .password("password")
                .tenantName("Tenant A")
                .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        jwtToken = result.getResponse().getContentAsString();
    }

    /**
     * Verifies that an authenticated user is blocked with HTTP 429
     * after exhausting their bucket of 100 requests.
     *
     * The first 100 requests drain the bucket. The 101st must be
     * rate-limited regardless of the endpoint responding.
     */
    @Test
    void authenticatedEndpoint_shouldReturn429AfterHundredRequests() throws Exception {
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/bookings/userBookings")
                            .header("Authorization", "Bearer " + jwtToken))
                    .andReturn();
        }

        mockMvc.perform(get("/bookings/userBookings")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * Verifies that two users have completely independent buckets.
     *
     * Exhausting Alice's bucket must have no effect on Ariana.
     * Each user is keyed by their own UUID inside RateLimitingFilter,
     * so draining one user's bucket never touches another user's.
     */
    @Test
    void twoUsers_shouldHaveIndependentBuckets() throws Exception {
        LoginRequest arianaLogin = LoginRequest.builder()
                .email("ariana@example.com")
                .password("password")
                .tenantName("Tenant A")
                .build();

        MvcResult arianaResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(arianaLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String arianaToken = arianaResult.getResponse().getContentAsString();

        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/bookings/userBookings")
                            .header("Authorization", "Bearer " + jwtToken))
                    .andReturn();
        }

        mockMvc.perform(get("/bookings/userBookings")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isTooManyRequests());

        MvcResult result = mockMvc.perform(get("/bookings/userBookings")
                        .header("Authorization", "Bearer " + arianaToken))
                .andReturn();

        assertNotEquals(429, result.getResponse().getStatus());
    }
}