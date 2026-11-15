package com.razza.bookingsystem.security;

import com.razza.bookingsystem.domain.Venue;
import com.razza.bookingsystem.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JwtAuthenticationFilter.
 *
 * No Spring context is started — the filter is instantiated directly with
 * mocked JwtService and CustomUserDetailsService, and called manually with
 * fake request/response objects.
 * The SecurityContext is cleared before and after each test,
 * so authentication state never leaks between tests.
 */
class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;
    private CustomUserDetails userDetails;

    /**
     * Creates a fresh filter with mocked dependencies before each test,
     * together with a default user that the mocked service can return.
     */
    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtService = mock(JwtService.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        Venue venue = Venue.builder().id(UUID.randomUUID()).name("VenueA").build();
        userDetails = new CustomUserDetails(
                UUID.randomUUID(), "user", null, "user@a.com", "hashedPassword",
                venue, null,
                List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
    }

    /**
     * Clears the SecurityContext so the authentication set by one test
     * is not visible to the next one.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifies that a request without the Authorization header goes through the
     * filter chain untouched: no authentication is set and the JWT service is not used.
     */
    @Transactional
    @Test
    void noAuthorizationHeader_shouldContinueChainWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    /**
     * Verifies that an Authorization header that is not of the Bearer kind
     * (for example Basic) is ignored by the filter.
     */
    @Transactional
    @Test
    void headerWithoutBearerPrefix_shouldContinueChainWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    /**
     * Verifies that a valid token sets the authentication in the SecurityContext:
     * the principal is the user loaded from the database, the authorities are the
     * ones extracted from the token, and the chain is continued.
     */
    @Transactional
    @Test
    void validToken_shouldSetAuthenticationInSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.extractSubject("valid.jwt.token")).thenReturn("user@a.com|VenueA");
        when(userDetailsService.loadUserByUsername("user@a.com|VenueA")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.jwt.token", userDetails)).thenReturn(true);
        when(jwtService.extractRoles("valid.jwt.token"))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertSame(userDetails, authentication.getPrincipal());
        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        verify(chain).doFilter(request, response);
    }

    /**
     * Verifies that a token that is not valid for the loaded user does not set
     * any authentication, but the chain is still continued
     * (the request will be rejected later by the authorization rules).
     */
    @Transactional
    @Test
    void invalidToken_shouldNotSetAuthentication_butContinueChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.extractSubject("invalid.jwt.token")).thenReturn("user@a.com|VenueA");
        when(userDetailsService.loadUserByUsername("user@a.com|VenueA")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid.jwt.token", userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractRoles(any());
        verify(chain).doFilter(request, response);
    }

    /**
     * Verifies that a token without a subject does not load any user from the
     * database and does not set any authentication.
     */
    @Transactional
    @Test
    void tokenWithoutSubject_shouldNotLoadUser_andNotSetAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        request.addHeader("Authorization", "Bearer no.subject.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.extractSubject("no.subject.token")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
        verify(chain).doFilter(request, response);
    }

    /**
     * Verifies that if the SecurityContext already contains an authentication,
     * the filter leaves it unchanged and does not load the user again.
     */
    @Transactional
    @Test
    void alreadyAuthenticated_shouldKeepExistingAuthentication() throws ServletException, IOException {
        Authentication existing = new UsernamePasswordAuthenticationToken(
                "existing", null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
        SecurityContextHolder.getContext().setAuthentication(existing);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.extractSubject("valid.jwt.token")).thenReturn("user@a.com|VenueA");

        filter.doFilterInternal(request, response, chain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
        verify(chain).doFilter(request, response);
    }
}