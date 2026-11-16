package com.razza.bookingsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.PerformanceDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.razza.bookingsystem.domain.Status.CONFIRMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the performance endpoints, exercising retrieval, creation,
 * deactivation and modification through MockMvc against the full Spring context with the test profile.
 *
 * Setup:
 * Creates two venues, VenueA and VenueB.
 * Creates a guest user, an admin and a performer in VenueA, and a performer in VenueB.
 * Creates a comedy performance belonging to the VenueA performer.
 *
 * Scenarios:
 * Each endpoint is called without authentication, as a guest, as a performer,
 * as an admin of the same venue, and as an admin targeting a user of a different venue.
 * Requests for a performance that does not exist are also covered.
 *
 * Expected outcome:
 * Requests without a token return 401.
 * Guests are not allowed to use any of the endpoints and get 403, leaving the data unchanged.
 * Performers can list, add, deactivate and modify their own performances, returning 200.
 * Admins can do the same on behalf of a user of their own venue, but must specify which user.
 * Any access to a user of another venue, or to a missing performance, returns 404.
 * Deleting a performance does not remove it, it only sets its type to INACTIVE.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PerformanceControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private VenueRepository venueRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private PerformanceRepository performanceRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Venue venueA;
    private Venue venueB;
    private User guestA;
    private User adminA;
    private User performerA;
    private User performerB;
    private Performance performanceA;

    @BeforeEach
    void setUp() {
        venueA = venueRepository.save(Venue.builder().name("VenueA").build());
        venueB = venueRepository.save(Venue.builder().name("VenueB").build());

        guestA = userRepository.save(User.builder()
                .email("guest@a.com").password(passwordEncoder.encode("pass"))
                .role(Role.GUEST).venue(venueA).status(CONFIRMED).build());

        adminA = userRepository.save(User.builder()
                .email("admin@a.com").password(passwordEncoder.encode("pass"))
                .role(Role.ADMIN).venue(venueA).status(CONFIRMED).build());

        performerA = userRepository.save(User.builder()
                .email("performer@a.com").password(passwordEncoder.encode("pass"))
                .role(Role.PERFORMER).venue(venueA).status(CONFIRMED).build());

        performerB = userRepository.save(User.builder()
                .email("performer@b.com").password(passwordEncoder.encode("pass"))
                .role(Role.PERFORMER).venue(venueB).status(CONFIRMED).build());

        performanceA = performanceRepository.save(Performance.builder()
                .user(performerA)
                .name("stand up")
                .description("a funny show")
                .duration(30)
                .performanceType(PerformanceType.COMEDY)
                .build());
    }

    /**
     * Builds the Authorization header value for the given user, consisting of the
     * Bearer prefix followed by a JWT generated from the user's details and role.
     */
    private String tokenFor(User user) {
        CustomUserDetails details = new CustomUserDetails(
                user.getId(), user.getName(), user.getBirthDate(), user.getEmail(), user.getPassword(),
                user.getVenue(), user.getAvailability(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        return "Bearer " + jwtService.generateToken(details);
    }

    /**
     * Saves a future event in VenueA with one slot assigned to the given performer.
     * The slot references performanceA.
     */
    private Event saveEventWithSlotFor(User performer) {
        OffsetDateTime start = OffsetDateTime.now().plusDays(3);
        TimeSlot slot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(start)
                .endTime(start.plusMinutes(30))
                .performanceId(performanceA.getId())
                .build();

        return eventRepository.save(Event.builder()
                .name("Open Mic")
                .description("desc")
                .location("Main Hall")
                .date(start)
                .bookingPolicy(new BookingPolicy(50, 50))
                .venue(venueA)
                .status(Status.CONFIRMED)
                .schedule(new ArrayList<>(List.of(slot)))
                .build());
    }

    /**
     * Builds a valid request body for a new poetry performance, used by the creation tests.
     */
    private PerformanceDto newPerformanceDto() {
        return PerformanceDto.builder()
                .name("new act")
                .description("something new")
                .duration(20)
                .performanceType(PerformanceType.POETRY)
                .build();
    }

    // GET /performances  (PERFORMER or ADMIN)

    @Transactional
    @Test
    void getPerformances_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/performances"))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void getPerformances_guest_returns403() throws Exception {
        mockMvc.perform(get("/performances")
                        .header("Authorization", tokenFor(guestA)))
                .andExpect(status().isForbidden());
    }

    @Transactional
    @Test
    void getPerformances_performer_returns200WithOwnScheduledPerformances() throws Exception {
        saveEventWithSlotFor(performerA);

        mockMvc.perform(get("/performances")
                        .header("Authorization", tokenFor(performerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("stand up"))
                .andExpect(jsonPath("$[0].location").value("Main Hall"));
    }

    @Transactional
    @Test
    void getPerformances_performerWithoutEvents_returns200WithEmptyList() throws Exception {
        mockMvc.perform(get("/performances")
                        .header("Authorization", tokenFor(performerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Transactional
    @Test
    void getPerformances_adminSameVenue_returns200WithRequestedUserPerformances() throws Exception {
        saveEventWithSlotFor(performerA);

        mockMvc.perform(get("/performances")
                        .param("userId", performerA.getId().toString())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("stand up"));
    }

    @Transactional
    @Test
    void getPerformances_adminDifferentVenue_returns404() throws Exception {
        mockMvc.perform(get("/performances")
                        .param("userId", performerB.getId().toString())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }

    @Transactional
    @Test
    void getPerformances_adminWithoutUserId_returns404() throws Exception {
        mockMvc.perform(get("/performances")
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }

    // POST /performances  (PERFORMER or ADMIN)

    @Transactional
    @Test
    void addPerformance_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/performances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPerformanceDto())))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void addPerformance_guest_returns403() throws Exception {

        long before = performanceRepository.count();

        mockMvc.perform(post("/performances")
                        .header("Authorization", tokenFor(guestA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPerformanceDto())))
                .andExpect(status().isForbidden());

        assertEquals(before, performanceRepository.count());        // only the one created in setUp()
    }

    @Transactional
    @Test
    void addPerformance_performer_returns200AndSavesPerformanceForHimself() throws Exception {

        long before = performanceRepository.count();

        mockMvc.perform(post("/performances")
                        .header("Authorization", tokenFor(performerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPerformanceDto())))
                .andExpect(status().isOk());

        assertEquals(before + 1, performanceRepository.count());
        Performance created = performanceRepository.findAll().stream()
                .filter(p -> p.getName().equals("new act"))
                .findFirst()
                .orElseThrow();
        assertEquals(performerA.getId(), created.getUser().getId());
        assertEquals(PerformanceType.POETRY, created.getPerformanceType());
    }

    @Transactional
    @Test
    void addPerformance_adminSameVenue_returns200AndSavesPerformanceForRequestedUser() throws Exception {
        mockMvc.perform(post("/performances")
                        .param("userId", performerA.getId().toString())
                        .header("Authorization", tokenFor(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPerformanceDto())))
                .andExpect(status().isOk());

        Performance created = performanceRepository.findAll().stream()
                .filter(p -> p.getName().equals("new act"))
                .findFirst()
                .orElseThrow();
        assertEquals(performerA.getId(), created.getUser().getId());
    }

    @Transactional
    @Test
    void addPerformance_adminDifferentVenue_returns404() throws Exception {

        long before = performanceRepository.count();

        mockMvc.perform(post("/performances")
                        .param("userId", performerB.getId().toString())
                        .header("Authorization", tokenFor(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPerformanceDto())))
                .andExpect(status().isNotFound());

        assertEquals(before, performanceRepository.count());
    }

    // DELETE /performances?performanceId=  (PERFORMER or ADMIN)

    @Transactional
    @Test
    void deletePerformance_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/performances")
                        .param("performanceId", performanceA.getId().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void deletePerformance_guest_returns403() throws Exception {
        mockMvc.perform(delete("/performances")
                        .param("performanceId", performanceA.getId().toString())
                        .header("Authorization", tokenFor(guestA)))
                .andExpect(status().isForbidden());

        assertEquals(PerformanceType.COMEDY,
                performanceRepository.findById(performanceA.getId()).orElseThrow().getPerformanceType());
    }

    @Transactional
    @Test
    void deletePerformance_performer_returns200AndSetsTypeToInactive() throws Exception {

        long before = performanceRepository.count();


        mockMvc.perform(delete("/performances")
                        .param("performanceId", performanceA.getId().toString())
                        .header("Authorization", tokenFor(performerA)))
                .andExpect(status().isOk());

        assertEquals(PerformanceType.INACTIVE,
                performanceRepository.findById(performanceA.getId()).orElseThrow().getPerformanceType());
        assertEquals(before, performanceRepository.count());         // not removed, only deactivated
    }

    @Transactional
    @Test
    void deletePerformance_nonExistentPerformance_returns404() throws Exception {
        mockMvc.perform(delete("/performances")
                        .param("performanceId", UUID.randomUUID().toString())
                        .header("Authorization", tokenFor(performerA)))
                .andExpect(status().isNotFound());
    }

    // PUT /performances/{performanceId}  (PERFORMER or ADMIN)

    @Transactional
    @Test
    void modifyPerformance_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/performances/{id}", performanceA.getId())
                        .param("performanceType", "LIVE_MUSIC")
                        .param("duration", "45"))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void modifyPerformance_guest_returns403() throws Exception {
        mockMvc.perform(put("/performances/{id}", performanceA.getId())
                        .param("performanceType", "LIVE_MUSIC")
                        .param("duration", "45")
                        .header("Authorization", tokenFor(guestA)))
                .andExpect(status().isForbidden());

        assertEquals(30, performanceRepository.findById(performanceA.getId()).orElseThrow().getDuration());
    }

    @Transactional
    @Test
    void modifyPerformance_performer_returns200AndUpdatesTypeAndDuration() throws Exception {
        mockMvc.perform(put("/performances/{id}", performanceA.getId())
                        .param("performanceType", "LIVE_MUSIC")
                        .param("duration", "45")
                        .header("Authorization", tokenFor(performerA)))
                .andExpect(status().isOk());

        Performance updated = performanceRepository.findById(performanceA.getId()).orElseThrow();
        assertEquals(PerformanceType.LIVE_MUSIC, updated.getPerformanceType());
        assertEquals(45, updated.getDuration());
    }

    @Transactional
    @Test
    void modifyPerformance_nonExistentPerformance_returns404() throws Exception {
        mockMvc.perform(put("/performances/{id}", UUID.randomUUID())
                        .param("performanceType", "LIVE_MUSIC")
                        .param("duration", "45")
                        .header("Authorization", tokenFor(performerA)))
                .andExpect(status().isNotFound());
    }
}