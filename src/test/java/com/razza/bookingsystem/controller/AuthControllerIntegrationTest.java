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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                .build());
    }

    // POST /auth/signup

    @Transactional
    @Test
    void signup_validRequest_returns200WithUserDto() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .email("newuser@a.com")
                .password("securepassword")
                .venueName("VenueA")
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
        SignupRequest request = SignupRequest.builder()
                .email("user@a.com")        // already exists in setUp()
                .password("anypassword")
                .venueName("VenueA")
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Transactional
    @Test
    void signup_nonExistentVenue_returns404() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .email("ghost@nowhere.com")
                .password("anypassword")
                .venueName("NonExistentVenue")
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
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