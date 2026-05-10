package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.repository.*;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.security.JwtService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc   // used for http endpoints testing, http requests can be sent without actually starting a server
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // used to restart spring application context after every test, startup can be expensive but this way state leakage (such as CRUD operations on bookings) is avoided between tests
class BookingControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
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
    private Booking bookingA;

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
                .name("Concert A").description("desc").location("Venue A")
                .date(OffsetDateTime.now().plusDays(3)).totalCapacity(100)
                .availableCapacity(100).tenant(tenantA).status(Status.CONFIRMED).build());

        eventB = eventRepository.save(Event.builder()
                .name("Concert B").description("desc").location("Venue B")
                .date(OffsetDateTime.now().plusDays(3)).totalCapacity(50)
                .availableCapacity(50).tenant(tenantB).status(Status.CONFIRMED).build());

        bookingA = bookingRepository.save(Booking.builder()
                .user(userA).event(eventA).tenant(tenantA).quantity(2)
                .status(Status.CONFIRMED).createdAt(OffsetDateTime.now()).build());
    }

    private String tokenFor(User user) {
        CustomUserDetails details = new CustomUserDetails(
                user.getId(), user.getEmail(), user.getPassword(), user.getTenant(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        return "Bearer " + jwtService.generateToken(details);
    }

    // POST /bookings/{eventId}/book

    @Transactional
    @Test
    void createBooking_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/bookings/{id}/book", eventA.getId()).param("quantity", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void createBooking_authenticatedUser_returns200() throws Exception {
        Event freshEvent = eventRepository.save(Event.builder()
                .name("Fresh").description("d").location("l")
                .date(OffsetDateTime.now().plusDays(5)).totalCapacity(10)
                .availableCapacity(10).tenant(tenantA).status(Status.CONFIRMED).build());

        mockMvc.perform(post("/bookings/{id}/book", freshEvent.getId())
                        .param("quantity", "1")
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.quantity").value(1));
    }

    @Transactional
    @Test
    void createBooking_crossTenant_returns404() throws Exception {
        mockMvc.perform(post("/bookings/{id}/book", eventB.getId())
                        .param("quantity", "1")
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isNotFound());
    }

    @Transactional
    @Test
    void createBooking_adminOnBehalfOfUserSameTenant_returns200() throws Exception {
        Event freshEvent = eventRepository.save(Event.builder()
                .name("Admin Booking").description("d").location("l")
                .date(OffsetDateTime.now().plusDays(5)).totalCapacity(10)
                .availableCapacity(10).tenant(tenantA).status(Status.CONFIRMED).build());

        mockMvc.perform(post("/bookings/{id}/book", freshEvent.getId())
                        .param("quantity", "2")
                        .param("userId", userA.getId().toString())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userA.getId().toString()));
    }

    @Transactional
    @Test
    void createBooking_adminOnBehalfOfUserDifferentTenant_returns404() throws Exception {
        mockMvc.perform(post("/bookings/{id}/book", eventA.getId())
                        .param("quantity", "1")
                        .param("userId", userB.getId().toString())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PUT /bookings/{bookingId}
    // -------------------------------------------------------------------------

    @Transactional
    @Test
    void modifyQuantity_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/bookings/{id}", bookingA.getId()).param("quantity", "3"))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void modifyQuantity_owner_returns200() throws Exception {
        mockMvc.perform(put("/bookings/{id}", bookingA.getId())
                        .param("quantity", "3")
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Transactional
    @Test
    void modifyQuantity_crossTenantUser_returns404() throws Exception {
        mockMvc.perform(put("/bookings/{id}", bookingA.getId())
                        .param("quantity", "1")
                        .header("Authorization", tokenFor(userB)))
                .andExpect(status().isNotFound());
    }

    @Transactional
    @Test
    void modifyQuantity_admin_returns200() throws Exception {
        mockMvc.perform(put("/bookings/{id}", bookingA.getId())
                        .param("quantity", "5")
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));
    }

    // DELETE /bookings/{id}

    @Transactional
    @Test
    void cancelBooking_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/bookings/{id}", bookingA.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void cancelBooking_owner_returns200() throws Exception {
        mockMvc.perform(delete("/bookings/{id}", bookingA.getId())
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isOk());
    }

    @Transactional
    @Test
    void cancelBooking_nonOwnerSameTenant_returns403() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other@a.com").password(passwordEncoder.encode("pass"))
                .role(Role.USER).tenant(tenantA).build());

        mockMvc.perform(delete("/bookings/{id}", bookingA.getId())
                        .header("Authorization", tokenFor(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Transactional
    @Test
    void cancelBooking_admin_returns200() throws Exception {
        mockMvc.perform(delete("/bookings/{id}", bookingA.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk());
    }

    @Transactional
    @Test
    void cancelBooking_crossTenant_returns404() throws Exception {
        mockMvc.perform(delete("/bookings/{id}", bookingA.getId())
                        .header("Authorization", tokenFor(userB)))
                .andExpect(status().isNotFound());
    }

    // GET /bookings/userBookings

    @Transactional
    @Test
    void getUserBookings_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/bookings/userBookings"))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void getUserBookings_user_returnsOwnBookings() throws Exception {
        mockMvc.perform(get("/bookings/userBookings")
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userA.getId().toString()));
    }

    @Transactional
    @Test
    void getUserBookings_adminQueryOtherUser_returns200() throws Exception {
        mockMvc.perform(get("/bookings/userBookings")
                        .param("userId", userA.getId().toString())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userA.getId().toString()));
    }

    @Transactional
    @Test
    void getUserBookings_adminQueryCrossTenantUser_returns404() throws Exception {
        mockMvc.perform(get("/bookings/userBookings")
                        .param("userId", userB.getId().toString())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }
}