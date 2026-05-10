package com.razza.bookingsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.repository.*;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.security.JwtService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenantA;
    private Tenant tenantB;
    private User userA;
    private User adminA;
    private User userB;
    private Event eventA;
    private Event eventB;
    private EventRequestDto validRequest;

    @BeforeEach
    void setUp() {
        tenantA = tenantRepository.save(Tenant.builder().name("TenantA").build());
        tenantB = tenantRepository.save(Tenant.builder().name("TenantB").build());

        userA = userRepository.save(User.builder()
                .email("user@a.com").password(passwordEncoder.encode("pass"))
                .role(Role.USER).tenant(tenantA).build());

        adminA = userRepository.save(User.builder()
                .email("admin@a.com").password(passwordEncoder.encode("pass"))
                .role(Role.ADMIN).tenant(tenantA).build());

        userB = userRepository.save(User.builder()
                .email("user@b.com").password(passwordEncoder.encode("pass"))
                .role(Role.USER).tenant(tenantB).build());

        eventA = eventRepository.save(Event.builder()
                .name("Event A").description("desc").location("Location A")
                .date(OffsetDateTime.now().plusDays(5)).totalCapacity(50)
                .availableCapacity(50).tenant(tenantA).status(Status.CONFIRMED).build());

        eventB = eventRepository.save(Event.builder()
                .name("Event B").description("desc").location("Location B")
                .date(OffsetDateTime.now().plusDays(5)).totalCapacity(30)
                .availableCapacity(30).tenant(tenantB).status(Status.CONFIRMED).build());

        validRequest = EventRequestDto.builder()
                .name("New Event").description("A great event").location("Main Hall")
                .date(OffsetDateTime.now().plusDays(10)).totalCapacity(100).build();
    }

    private String tokenFor(User user) {
        CustomUserDetails details = new CustomUserDetails(
                user.getId(), user.getEmail(), user.getPassword(), user.getTenant(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        return "Bearer " + jwtService.generateToken(details);
    }

    // POST /events  (ADMIN only)

    @Transactional
    @Test
    void createEvent_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void createEvent_regularUser_returns403() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isForbidden());
    }

    @Transactional
    @Test
    void createEvent_admin_returns200() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Event"))
                .andExpect(jsonPath("$.availableCapacity").value(100));
    }

    // GET /events

    @Transactional
    @Test
    void getAllEvents_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void getAllEvents_returnsOnlyOwnTenantEvents() throws Exception {
        mockMvc.perform(get("/events").header("Authorization", tokenFor(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name == 'Event A')]").exists())
                .andExpect(jsonPath("$.content[?(@.name == 'Event B')]").doesNotExist());
    }

    // GET /events/{id}

    @Transactional
    @Test
    void getEventById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/events/{id}", eventA.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void getEventById_ownTenant_returns200() throws Exception {
        mockMvc.perform(get("/events/{id}", eventA.getId())
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventA.getId().toString()));
    }

    @Transactional
    @Test
    void getEventById_crossTenant_returns404() throws Exception {
        mockMvc.perform(get("/events/{id}", eventB.getId())
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isNotFound());
    }

    @Transactional
    @Test
    void getEventById_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/events/{id}", UUID.randomUUID())
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isNotFound());
    }

    // PUT /events/{id}  (ADMIN only)

    @Transactional
    @Test
    void updateEvent_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/events/{id}", eventA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void updateEvent_regularUser_returns403() throws Exception {
        mockMvc.perform(put("/events/{id}", eventA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isForbidden());
    }

    @Transactional
    @Test
    void updateEvent_admin_returns200() throws Exception {
        EventRequestDto update = EventRequestDto.builder()
                .name("Updated Name").description("updated desc")
                .location(eventA.getLocation()).date(eventA.getDate())
                .totalCapacity(50).build();

        mockMvc.perform(put("/events/{id}", eventA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Transactional
    @Test
    void updateEvent_crossTenant_returns404() throws Exception {
        mockMvc.perform(put("/events/{id}", eventB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }

    // DELETE /events/{id}  (ADMIN only)

    @Transactional
    @Test
    void deleteEvent_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/events/{id}", eventA.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void deleteEvent_regularUser_returns403() throws Exception {
        mockMvc.perform(delete("/events/{id}", eventA.getId())
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isForbidden());
    }

    @Transactional
    @Test
    void deleteEvent_adminNoBookings_returns200() throws Exception {
        mockMvc.perform(delete("/events/{id}", eventA.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk());
    }

    @Transactional
    @Test
    void deleteEvent_adminWithActiveBookings_returns409() throws Exception {
        bookingRepository.save(Booking.builder()
                .user(userA).event(eventA).tenant(tenantA).quantity(1)
                .status(Status.CONFIRMED).createdAt(OffsetDateTime.now()).build());

        mockMvc.perform(delete("/events/{id}", eventA.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isConflict());
    }

    @Transactional
    @Test
    void deleteEvent_crossTenant_returns404() throws Exception {
        mockMvc.perform(delete("/events/{id}", eventB.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }
}