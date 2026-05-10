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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerIntegrationTest {

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
    }

    private String tokenFor(User user) {
        CustomUserDetails details = new CustomUserDetails(
                user.getId(), user.getEmail(), user.getPassword(), user.getTenant(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        return "Bearer " + jwtService.generateToken(details);
    }

    // PUT /users/{userId}  — promote to admin (ADMIN only)

    @Transactional
    @Test
    void makeAdmin_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/users/{id}", userA.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void makeAdmin_regularUser_returns403() throws Exception {
        mockMvc.perform(put("/users/{id}", userA.getId())
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isForbidden());
    }

    @Transactional
    @Test
    void makeAdmin_adminSameTenant_returns200WithAdminRole() throws Exception {
        mockMvc.perform(put("/users/{id}", userA.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(Role.ADMIN.name()));        // since role is an enum, .name() is used, Role.ADMIN.name() -> returns: ADMIN
    }

    @Transactional
    @Test
    void makeAdmin_adminDifferentTenant_returns404() throws Exception {
        mockMvc.perform(put("/users/{id}", userB.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }

    @Transactional
    @Test
    void makeAdmin_nonExistentUser_returns404() throws Exception {
        mockMvc.perform(put("/users/{id}", UUID.randomUUID())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }

    // DELETE /users/{userId}  (ADMIN only)

    @Transactional
    @Test
    void deleteUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/users/{id}", userA.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Transactional
    @Test
    void deleteUser_regularUser_returns403() throws Exception {
        mockMvc.perform(delete("/users/{id}", userA.getId())
                        .header("Authorization", tokenFor(userA)))
                .andExpect(status().isForbidden());
    }

    @Transactional
    @Test
    void deleteUser_adminNoBookings_returns200() throws Exception {
        mockMvc.perform(delete("/users/{id}", userA.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isOk());
    }

    @Transactional
    @Test
    void deleteUser_adminWithActiveBookings_returns409() throws Exception {
        Event event = eventRepository.save(Event.builder()
                .name("E").description("d").location("l")
                .date(OffsetDateTime.now().plusDays(1)).totalCapacity(10)
                .availableCapacity(10).tenant(tenantA).status(Status.CONFIRMED).build());

        bookingRepository.save(Booking.builder()
                .user(userA).event(event).tenant(tenantA).quantity(1)
                .status(Status.CONFIRMED).createdAt(OffsetDateTime.now()).build());

        mockMvc.perform(delete("/users/{id}", userA.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isConflict());
    }

    @Transactional
    @Test
    void deleteUser_adminDifferentTenant_returns404() throws Exception {
        mockMvc.perform(delete("/users/{id}", userB.getId())
                        .header("Authorization", tokenFor(adminA)))
                .andExpect(status().isNotFound());
    }
}