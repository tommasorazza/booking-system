package com.razza.bookingsystem.config;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Initializes sample data in the database at application startup.
 *
 * This class uses a {@link CommandLineRunner} bean to insert:
 *  Tenants
 *  Users (with encoded passwords)
 *  Events
 *  Bookings
 * It is primarily intended for development and testing purposes.
 * In production, users would typically sign up via the API, and
 * bookings would be created via application logic.
 */
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a {@link CommandLineRunner} that seeds initial data into the database.
     *
     * This runner executes automatically after the Spring context is initialized.
     * It creates:
         Two tenants (Tenant A and Tenant B)
     *   Three users (Alice, Bob, Admin) with passwords encoded via {@link PasswordEncoder}
     *   Two events, each linked to a tenant
     *   Two bookings, each linking a user and an event
     *
     * @return a {@link CommandLineRunner} that seeds the database
     */
    @Bean
    CommandLineRunner initData() {
        return args -> {

            Tenant tenantA = new Tenant(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "Tenant A"
            );

            Tenant tenantB = new Tenant(
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    "Tenant B"
            );

            tenantRepository.save(tenantA);
            tenantRepository.save(tenantB);

            User alice = new User(
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                    "alice@example.com",
                    passwordEncoder.encode("password"),
                    Role.USER,
                    tenantA.getId()
            );

            User bob = new User(
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                    "bob@example.com",
                    passwordEncoder.encode("password"),
                    Role.USER,
                    tenantB.getId()
            );

            User admin = new User(
                    UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                    "admin@example.com",
                    passwordEncoder.encode("admin_pass"),
                    Role.ADMIN,
                    tenantA.getId()
            );

            userRepository.save(alice);
            userRepository.save(bob);
            userRepository.save(admin);

            Event event1 = new Event(
                    UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"),
                    "Spring Boot Workshop",
                    "Learn Spring Boot basics",
                    "Room 101",
                    LocalDateTime.now().plusDays(1),
                    30,
                    30,
                    0L,
                    tenantA.getId()
            );

            Event event2 = new Event(
                    UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                    "Docker Deep Dive",
                    "Hands-on Docker session",
                    "Room 202",
                    LocalDateTime.now().plusDays(2),
                    25,
                    25,
                    0L,
                    tenantB.getId()
            );

            eventRepository.save(event1);
            eventRepository.save(event2);

            Booking booking1 = new Booking(
                    UUID.fromString("99999999-9999-9999-9999-999999999999"),
                    alice.getId(),
                    event1.getId(),
                    tenantA.getId(),
                    2,
                    Status.CONFIRMED,
                    LocalDateTime.now()
            );

            Booking booking2 = new Booking(
                    UUID.fromString("88888888-8888-8888-8888-888888888888"),
                    bob.getId(),
                    event2.getId(),
                    tenantB.getId(),
                    1,
                    Status.CONFIRMED,
                    LocalDateTime.now()
            );

            bookingRepository.save(booking1);
            bookingRepository.save(booking2);
        };
    }
}