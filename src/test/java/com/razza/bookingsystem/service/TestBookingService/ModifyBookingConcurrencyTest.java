package com.razza.bookingsystem.service.TestBookingService;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.TenantRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.service.BookingService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests concurrent modifications of a booking quantity by multiple admins.
 *
 * Scenario:
 * - A booking initially has quantity = 2
 * - Event has sufficient capacity
 * - 10 admins attempt to modify the booking concurrently
 *
 * Expected outcome:
 * - Multiple modifications may succeed due to optimistic locking retries
 * - Final booking quantity must be one of the requested values
 * - Event available capacity must remain consistent:
 *   totalCapacity - finalBookingQuantity
 *
 * This ensures that concurrent modifications do not corrupt capacity.
 */
@SpringBootTest
@ActiveProfiles("test")
class ModifyBookingConcurrencyTest {

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
    void modify_booking_concurrently_should_keep_capacity_consistent() throws Exception {

        Tenant tenant = new Tenant();
        tenant = tenantRepository.save(tenant);

        Event event = Event.builder()
                .name("Modify Concurrency Test")
                .date(OffsetDateTime.now().plusDays(1))
                .availableCapacity(98)
                .totalCapacity(100)
                .tenant(tenant)
                .build();

        event = eventRepository.save(event);

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
            final int newQuantity = i + 1;

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();

                        bookingService.modifyQuantity(
                                bookingId,
                                admin,
                                admin.getTenant(),
                                newQuantity,
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

        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        Booking updatedBooking = bookingRepository.findById(bookingId).orElseThrow();

        int finalQuantity = updatedBooking.getQuantity();

        System.out.println("SUCCESS: " + success.get());
        System.out.println("FAIL: " + fail.get());
        System.out.println("FINAL QUANTITY: " + finalQuantity);

        /**
         * The final quantity must be between 1 and 10
         */
        boolean validQuantity = finalQuantity >= 1 && finalQuantity <= 10;
        assertEquals(true, validQuantity, "Final quantity must be within expected range");

        int expectedAvailable = updatedEvent.getTotalCapacity() - finalQuantity;

        assertEquals(
                expectedAvailable,
                updatedEvent.getAvailableCapacity(),
                "Event capacity must remain consistent"
        );
    }
}