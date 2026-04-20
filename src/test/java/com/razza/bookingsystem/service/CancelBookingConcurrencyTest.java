package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.TenantRepository;
import com.razza.bookingsystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that concurrent cancellation attempts on the same booking are handled correctly.
 *
 * Setup:
 * - Creates a tenant, an event with 18 available seats out of 20 total
 * - Creates 10 admin users belonging to the same tenant
 * - Creates a confirmed booking of 2 seats for the first admin user
 *
 * Execution:
 * - Spawns 10 threads, each attempting to cancel the same booking simultaneously
 * - All threads are held at a latch and released at the same time to maximize contention
 *
 * Expected outcome:
 * - Exactly 1 cancellation succeeds
 * - The remaining 9 attempts fail
 * - Event available capacity is restored by exactly 2 (the booked quantity)
 * - The booking status is CANCELLED
 *
 * @throws Exception if thread coordination fails
 */
@SpringBootTest
@ActiveProfiles("test")
class CancelBookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void cancel_same_booking_concurrently_should_restore_capacity_once() throws Exception {

        Tenant tenant = new Tenant();
        tenant = tenantRepository.save(tenant);

        Event event = Event.builder()
                .name("Concurrency Cancel Test")
                .date(OffsetDateTime.now().plusDays(1))
                .availableCapacity(18)
                .totalCapacity(20)
                .tenant(tenant)
                .build();

        event = eventRepository.save(event);

        int initialCapacity = event.getAvailableCapacity();

        List<User> admins = new ArrayList<User>();

        for (int i = 0; i < 10; i++) {
            User admin = new User();
            admin.setEmail("admin" + i + "@test.com");
            admin.setPassword("password");
            admin.setRole(Role.ADMIN);
            admin.setTenant(tenant);

            admins.add(userRepository.save(admin));
        }

        User bookingUser = admins.get(0);

        Booking booking = Booking.builder()
                .user(bookingUser)
                .event(event)
                .tenant(tenant)
                .quantity(2)
                .status(Status.CONFIRMED)
                .createdAt(OffsetDateTime.now())
                .build();

        booking = bookingRepository.save(booking);

        UUID bookingId = booking.getId();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (int i = 0; i < admins.size(); i++) {

            final User admin = admins.get(i);

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();

                        bookingService.cancelBooking(
                                bookingId,
                                admin,
                                admin.getTenant(),
                                true
                        );

                        success.incrementAndGet();

                    } catch (Exception e) {
                        fail.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                }
            };

            executor.submit(task);
        }

        startLatch.countDown();
        doneLatch.await();

        executor.shutdown();

        Event updated = eventRepository.findById(event.getId()).orElseThrow();

        System.out.println("SUCCESS: " + success.get());
        System.out.println("FAIL: " + fail.get());

        assertEquals(1, success.get(), "Only one cancellation should succeed");
        assertEquals(9, fail.get(), "All other cancellations should fail");

        assertEquals(
                initialCapacity + 2,
                updated.getAvailableCapacity(),
                "Capacity must be restored exactly once"
        );

        Booking updatedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(Status.CANCELLED, updatedBooking.getStatus());
    }
}