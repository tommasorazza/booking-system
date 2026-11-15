package com.razza.bookingsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Venue;
import com.razza.bookingsystem.dto.SignupRequest;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.repository.VenueRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the validation of the POST /auth/signup request body.
 *
 * Every test starts from a fully valid request and changes only the field under test,
 * so a 400 can only be caused by that field.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SignupValidationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VenueRepository venueRepository;
    @Autowired private UserRepository userRepository;

    private Venue venueA;

    @BeforeEach
    void setUp() {
        venueA = venueRepository.save(Venue.builder().name("VenueA").build());
    }

    /**
     * Returns a builder already filled with valid values for every field,
     * each test overrides only the field it wants to break.
     */
    private SignupRequest.SignupRequestBuilder validRequest() {
        return SignupRequest.builder()
                .name("new user")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00.000+02:00"))
                .email("newuser@a.com")
                .password("securepassword")
                .role(Role.GUEST)
                .venueName("VenueA");
    }

    // valid request

    @Transactional
    @Test
    void signup_validRequest_returns200WithUserDto() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newuser@a.com"))
                .andExpect(jsonPath("$.role").value("GUEST"));
    }

    // missing or blank fields

    @Transactional
    @Test
    void signup_missingName_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().name(null).build())))
                .andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    void signup_blankName_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().name("   ").build())))
                .andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    void signup_missingBirthDate_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().birthDate(null).build())))
                .andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    void signup_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().email(null).build())))
                .andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    void signup_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().email("not-an-email").build())))
                .andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    void signup_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().password(null).build())))
                .andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    void signup_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().password("   ").build())))
                .andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    void signup_missingRole_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().role(null).build())))
                .andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    void signup_missingVenueName_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().venueName(null).build())))
                .andExpect(status().isBadRequest());
    }

    /**
     * A request rejected by the validation must not create the user,
     * and the response body must contain the status 400.
     */
    @Transactional
    @Test
    void signup_invalidRequest_doesNotSaveUser() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().email("not-an-email").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertFalse(userRepository.findByEmailAndVenue("not-an-email", venueA).isPresent());
    }

    // role

    @Transactional
    @Test
    void signup_roleNotInEnum_returns400WithValueAndEnumName() throws Exception {
        String json = "{\"name\":\"new user\","
                + "\"birthDate\":\"2000-01-01T00:00:00.000+02:00\","
                + "\"email\":\"newuser@a.com\","
                + "\"password\":\"securepassword\","
                + "\"role\":\"WAITER\","
                + "\"venueName\":\"VenueA\"}";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value 'WAITER' for Role"));
    }

    @Transactional
    @Test
    void signup_adminRole_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().role(Role.ADMIN).build())))
                .andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    void signup_performerWithoutAvailability_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest().role(Role.PERFORMER).build())))
                .andExpect(status().isBadRequest());
    }
}