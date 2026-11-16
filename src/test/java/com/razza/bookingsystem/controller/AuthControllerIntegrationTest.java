package com.razza.bookingsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Venue;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.LoginRequest;
import com.razza.bookingsystem.dto.SignupRequest;
import com.razza.bookingsystem.repository.VenueRepository;
import com.razza.bookingsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static com.razza.bookingsystem.domain.Status.CONFIRMED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the authentication endpoints, exercising signup and login
 * through MockMvc against the full Spring context with the test profile.
 *
 * Setup:
 * Creates a venue named VenueA and a confirmed guest user belonging to it.
 *
 * Signup scenarios:
 * A valid request, a request with an already registered email, a request for a venue
 * that does not exist, and a request with a missing or malformed body.
 *
 * Login scenarios:
 * Valid credentials, a wrong password, an unknown email, and a venue the user does not belong to.
 *
 * Expected outcome:
 * A valid signup returns 200 with the created user data.
 * A duplicate email returns 409, an unknown venue returns 404 and an invalid body returns 400.
 * A valid login returns 200 with a non empty token.
 * A wrong password, an unknown email and a wrong venue all return 401.
 *
 * Returning the same 401 whether or not the email exists ensures that no information
 * about user existence is leaked to potential attackers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VenueRepository venueRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Venue venueA;
    private User existingUser;

    @BeforeEach
    void setUp() {
        venueA = venueRepository.save(Venue.builder().name("VenueA").build());

        existingUser = userRepository.save(User.builder()
                .email("user@a.com")
                .password(passwordEncoder.encode("correctpassword"))
                .role(Role.GUEST)
                .venue(venueA)
                .status(CONFIRMED)
                .build());
    }

    /**
     * Builds a complete, valid signup body. Tests override only the fields they care about,
     * because @Valid on the controller rejects incomplete requests with 400.
     */
    private SignupRequest.SignupRequestBuilder validSignup() {
        return SignupRequest.builder()
                .name("New User")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .role(Role.GUEST)
                .password("securepassword")
                .venueName("VenueA");
    }

    // POST /auth/signup

    @Transactional
    @Test
    void signup_validRequest_returns200WithUserDto() throws Exception {
        SignupRequest request = validSignup()
                .email("newuser@a.com")
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newuser@a.com"))
                .andExpect(jsonPath("$.role").value("GUEST"));
    }

    @Transactional
    @Test
    void signup_duplicateEmail_returns409() throws Exception {
        SignupRequest request = validSignup()
                .email("user@a.com")        // already exists in setUp()
                .password("anypassword")
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Transactional
    @Test
    void signup_nonExistentVenue_returns404() throws Exception {
        SignupRequest request = validSignup()
                .email("ghost@nowhere.com")
                .password("anypassword")
                .venueName("NonExistentVenue")
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Transactional
    @Test
    void signup_invalidBody_returns400() throws Exception {
        // missing name, birthDate, role, password and venueName, and the email is malformed
        SignupRequest request = SignupRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // POST /auth/login

    @Transactional
    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@a.com")
                .password("correctpassword")
                .venueName("VenueA")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString()))); // spring mockMvc content() delegates the matching operations to library hamcrest
    }

    @Transactional
    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@a.com")
                .password("wrongpassword")
                .venueName("VenueA")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void login_nonExistentEmail_returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nobody@a.com")
                .password("anypassword")
                .venueName("VenueA")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());      //unauthorized both if email exists or not, this is username enumeration protection, meaning no information over user existence is leaked to potential attackers
    }

    @Transactional
    @Test
    void login_wrongVenue_returns401() throws Exception {
        // user@a.com exists in VenueA, not VenueB
        venueRepository.save(Venue.builder().name("VenueB").build());

        LoginRequest request = LoginRequest.builder()
                .email("user@a.com")
                .password("correctpassword")
                .venueName("VenueB")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}