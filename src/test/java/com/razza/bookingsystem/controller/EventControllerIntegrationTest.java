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
import java.util.HashSet;
import java.util.Set;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.razza.bookingsystem.domain.Status.CONFIRMED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the event endpoints, exercising creation, retrieval, update
 * and deletion through MockMvc against the full Spring context with the test profile.
 *
 * Setup:
 * Creates two venues, VenueA and VenueB.
 * Creates a guest user and an admin in VenueA, and a guest user in VenueB.
 * Creates a performer in VenueA, available every day of the week, with a jazz performance.
 * Creates one confirmed event per venue, each scheduled with the performer.
 * Prepares a valid event request whose first time slot starts exactly at the event date.
 *
 * Scenarios:
 * Each endpoint is called without authentication, as a regular user, as an admin
 * of the same venue, and as an admin of a different venue.
 *
 * Expected outcome:
 * Requests without a token return 401.
 * Create, update and delete are restricted to admins, so a regular user gets 403.
 * An admin can create and update events of their own venue, returning 200.
 * Deleting an event that has active bookings returns 409.
 * Listing events returns only the events of the caller's venue.
 * Any access to an event of another venue returns 404, so that the existence of
 * resources in other venues is not revealed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private VenueRepository venueRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PerformanceRepository performanceRepository;

    private Venue venueA;
    private Venue venueB;
    private User userA;
    private User adminA;
    private User userB;
    private Event eventA;
    private Event eventB;
    private EventRequestDto validRequest;

    @BeforeEach
    void setUp() {
        venueA = venueRepository.save(Venue.builder().name("VenueA").build());
        venueB = venueRepository.save(Venue.builder().name("VenueB").build());

        userA = userRepository.save(User.builder()
                .name("UserA").birthDate(OffsetDateTime.parse("2000-01-01T00:00:00+02:00"))
                .email("user@a.com").password(passwordEncoder.encode("pass"))
                .role(Role.GUEST).venue(venueA).status(CONFIRMED).build());

        adminA = userRepository.save(User.builder()
                .email("admin@a.com").password(passwordEncoder.encode("pass"))
                .role(Role.ADMIN).venue(venueA).status(CONFIRMED).build());

        userB = userRepository.save(User.builder()
                .email("user@b.com").password(passwordEncoder.encode("pass"))
                .role(Role.GUEST).venue(venueB).status(CONFIRMED).build());

        // Performer in the SAME venue as the admin, available every day of the week
        Performance jazz = Performance.builder()
                .name("jazz")
                .description("jazz session")
                .duration(120)
                .performanceType(PerformanceType.LIVE_MUSIC)
                .build();

        User performer = User.builder()
                .name("performer")
                .email("performer@a.com")
                .password(passwordEncoder.encode("pass"))
                .role(Role.PERFORMER)
                .venue(venueA)
                .status(CONFIRMED)
                .performances(new HashSet<>(Set.of(jazz)))
                .availability(new Availability(true, true, true, true, true, true, true))
                .build();
        performer.getAvailability().setUser(performer);
        performer = userRepository.save(performer);

        jazz.setUser(performer);
        jazz = performanceRepository.save(jazz);

        eventA = eventRepository.save(Event.builder()
                .name("Event A")
                .description("desc")
                .location("Location A")
                .date(OffsetDateTime.now().plusDays(5))
                .bookingPolicy(new BookingPolicy(50, 50))
                .venue(venueA)
                .status(CONFIRMED)
                .schedule(List.of(TimeSlot.builder()
                        .userEmail("performer@a.com")
                        .startTime(OffsetDateTime.now().plusDays(5))
                        .endTime(OffsetDateTime.now().plusDays(5).plusHours(2))
                        .performanceId(jazz.getId())
                        .build()))
                .eighteenPlus(false)
                .build());

        eventB = eventRepository.save(Event.builder()
                .name("Event B")
                .description("desc")
                .location("Location B")
                .date(OffsetDateTime.now().plusDays(5))
                .bookingPolicy(new BookingPolicy(30, 30))
                .venue(venueB)
                .status(CONFIRMED)
                .schedule(List.of(TimeSlot.builder()
                        .userEmail("performer@a.com")
                        .startTime(OffsetDateTime.now().plusDays(5))
                        .endTime(OffsetDateTime.now().plusDays(5).plusHours(2))
                        .performanceId(jazz.getId())
                        .build())).build());

        // One date reused everywhere: the first slot must start exactly at the event date
        OffsetDateTime eventDate = OffsetDateTime.now().plusDays(10);

        validRequest = EventRequestDto.builder()
                .name("New Event")
                .description("A great event")
                .location("Main Hall")
                .date(eventDate)
                .totalCapacity(100)
                .schedule(List.of(
                        TimeSlot.builder()
                                .userEmail("performer@a.com")
                                .startTime(eventDate)
                                .endTime(eventDate.plusHours(2))
                                .performanceId(jazz.getId())
                                .build()))
                .eighteenPlus(false)
                .build();
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
                .andDo(print())
                .andDo(result -> {
                    if (result.getResolvedException() != null) {
                        result.getResolvedException().printStackTrace();
                    }
                })                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Event"))
                .andExpect(jsonPath("$.totalCapacity").value(100));
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
    void getAllEvents_returnsOnlyOwnVenueEvents() throws Exception {
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
                .totalCapacity(50)
                .schedule(validRequest.getSchedule().stream()
                        .map(s -> TimeSlot.builder()
                                .userEmail(s.getUserEmail())
                                .startTime(eventA.getDate())
                                .endTime(eventA.getDate().plusHours(2))
                                .performanceId(s.getPerformanceId())
                                .build())
                        .toList())
                .eighteenPlus(false)
                .build();

        mockMvc.perform(put("/events/{id}", eventA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Transactional
    @Test
    void updateEvent_crossVenue_returns404() throws Exception {
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
    void deleteEvent_adminWithActiveBookings_returns409() throws Exception {
        bookingRepository.save(Booking.builder()
                .user(userA).event(eventA).venue(venueA).quantity(1)
                .status(CONFIRMED).createdAt(OffsetDateTime.now()).build());

        mockMvc.perform(delete("/events/{id}", eventA.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isConflict());
    }

    @Transactional
    @Test
    void deleteEvent_crossVenue_returns404() throws Exception {
        mockMvc.perform(delete("/events/{id}", eventB.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }
}