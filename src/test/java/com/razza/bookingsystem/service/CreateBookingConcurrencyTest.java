package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.exception.BookingAlreadyPresentException;
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
 * Integration test that verifies concurrent booking creation for the same user and event.
 *
 * Scenario:
 * - A single user attempts to create multiple bookings for the same event concurrently.
 * - The event has sufficient capacity initially.
 *
 * Concurrency model:
 * - 10 threads are executed in parallel using an ExecutorService.
 * - A CountDownLatch is used to start all threads simultaneously to maximize race conditions.
 * - Each thread calls createBooking with the same user and event.
 *
 * Expected behavior:
 * - Only one booking should succeed due to the unique constraint and business validation.
 * - All other concurrent attempts should fail.
 * - Either a BookingAlreadyPresentException or a DataIntegrityViolationException may occur.
 *
 * Assertions:
 * - Exactly one successful booking creation.
 * - Nine failed attempts.
 * - Only one booking persisted in the database for the event.
 * - Event capacity is reduced correctly by one unit.
 * - All thrown exceptions are of expected types.
 *
 * @throws Exception if thread synchronization or execution fails
 */
@SpringBootTest
@ActiveProfiles("test")
class CreateBookingConcurrencyTest {

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
    void same_user_concurrent_booking_should_create_only_one_booking() throws Exception {

        final Tenant tenant = tenantRepository.save(new Tenant());


        final User user = userRepository.save(User.builder()
                .email("user@test.com")
                .password("password")
                .role(Role.USER)
                .tenant(tenant)
                .build());

        Event event = Event.builder()
                .name("Concurrency Booking Test")
                .date(OffsetDateTime.now().plusDays(1))
                .availableCapacity(10)
                .totalCapacity(10)
                .tenant(tenant)
                .build();

        event = eventRepository.save(event);

        UUID eventId = event.getId();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();

                        bookingService.createBooking(
                                eventId,
                                user,
                                user,
                                tenant,
                                1,
                                false
                        );

                        success.incrementAndGet();

                    } catch (Exception e) {
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
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

        System.out.println("SUCCESS: " + success.get());
        System.out.println("FAIL: " + fail.get());

        assertEquals(1, success.get(), "Only one booking should succeed");
        assertEquals(9, fail.get(), "All other bookings should fail");

        List<Booking> bookings = (List<Booking>) bookingRepository.findByEventId(eventId);
        assertEquals(1, bookings.size(), "Only one booking should exist");

        Event updatedEvent = eventRepository.findById(eventId).orElseThrow();
        assertEquals(9, updatedEvent.getAvailableCapacity());

        for (Exception ex : exceptions) {
            boolean valid =
                    (ex instanceof BookingAlreadyPresentException) ||
                            (ex.getCause() instanceof org.springframework.dao.DataIntegrityViolationException);

            assertTrue(valid, "Unexpected exception type: " + ex.getClass());
        }
    }
}