package com.razza.bookingsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for RateLimitingFilter.
 *
 * No Spring context is started — the filter is instantiated directly
 * and called manually with fake request/response objects.
 * Each test gets a fresh filter instance via @BeforeEach,
 * so bucket state never leaks between tests.
 */
class RateLimitingFilterUnitTest {

    private RateLimitingFilter filter;

    /**
     * Creates a fresh RateLimitingFilter before each test.
     * This resets the internal bucket map so tests are fully isolated.
     */
    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
    }

    /**
     * Verifies that the auth endpoint allows the first 10 requests from an IP,
     * then blocks the 11th with HTTP 429.
     *
     * Also verifies that the filter chain was continued exactly 10 times,
     * confirming the blocked request never reached the controller.
     */
    @Test
    void authEndpoint_shouldAllowFirstTenRequests_thenBlock() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, chain);
            assertNotEquals(429, response.getStatus());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, chain);
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("Too many requests"));

        verify(chain, times(10)).doFilter(any(), any());
    }

    /**
     * Verifies that each IP address has its own independent bucket.
     *
     * Exhausting the bucket for IP 10.0.0.1 must have no effect on IP 10.0.0.2,
     * which should still be able to make requests freely.
     */
    @Test
    void differentIps_shouldHaveIndependentBuckets() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr("10.0.0.1");
            filter.doFilterInternal(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/auth/login");
        blockedReq.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse blockedResp = new MockHttpServletResponse();
        filter.doFilterInternal(blockedReq, blockedResp, chain);
        assertEquals(429, blockedResp.getStatus());

        MockHttpServletRequest otherReq = new MockHttpServletRequest("POST", "/auth/login");
        otherReq.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse otherResp = new MockHttpServletResponse();
        filter.doFilterInternal(otherReq, otherResp, chain);
        assertNotEquals(429, otherResp.getStatus());
    }
}