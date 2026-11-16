package com.razza.bookingsystem.concurrency.booking;

import com.razza.bookingsystem.concurrency.PostgresConcurrencyTestBase;
import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.VenueRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Integration test that verifies concurrent cancellation of different bookings
 * belonging to different users for the same event.
 *
 * Scenario:
 * 5 different users each have an existing confirmed booking for the same event.
 * All bookings are cancelled concurrently.
 *
 * Concurrency model:
 * 5 threads are executed in parallel using an ExecutorService.
 * A CountDownLatch is used to ensure all threads start at the same time.
 * Each thread cancels a different booking belonging to a different user.
 *
 * Expected outcome:
 * All 5 cancellation attempts succeed and no failures occur.
 * No race conditions or locking conflicts arise between independent bookings.
 * Event available capacity is restored by exactly the sum of all cancelled bookings (5 in this case).
 * All bookings in the database are marked as CANCELLED.
 *
 * This test ensures that event capacity updates remain safe under concurrent access
 * when multiple users cancel different bookings simultaneously.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CancelDifferentBookingsConcurrencyTest extends PostgresConcurrencyTestBase {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @AfterEach
    void cleanup() {
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        venueRepository.deleteAll();
    }

    @Test
    void concurrent_cancellation_of_different_bookings_should_restore_capacity_correctly() throws Exception {

        final Venue venue = venueRepository.save(new Venue());

        Event event = Event.builder()
                .name("Concurrent Cancel Multiple Bookings")
                .date(OffsetDateTime.now().plusDays(1))
                .bookingPolicy(new BookingPolicy(10,0))
                .venue(venue)
                .build();

        event = eventRepository.save(event);

        List<User> users = new ArrayList<User>();
        List<Booking> bookings = new ArrayList<Booking>();

        for (int i = 0; i < 5; i++) {

            User user = new User();
            user.setEmail("user" + i + "@test.com");
            user.setPassword("password");
            user.setRole(Role.GUEST);
            user.setVenue(venue);
            user = userRepository.save(user);

            users.add(user);

            Booking booking = Booking.builder()
                    .user(user)
                    .event(event)
                    .venue(venue)
                    .quantity(1)
                    .status(Status.CONFIRMED)
                    .createdAt(OffsetDateTime.now())
                    .build();

            bookings.add(bookingRepository.save(booking));
        }

        int threads = bookings.size();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (int i = 0; i < bookings.size(); i++) {

            final Booking booking = bookings.get(i);
            final User user = users.get(i);

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();

                        bookingService.cancelBooking(
                                booking.getId(),
                                user,
                                venue,
                                false
                        );

                        success.incrementAndGet();

                    } catch (Exception e) {
                        fail.incrementAndGet();
                        e.printStackTrace();
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

        System.out.println("SUCCESS: " + success.get());
        System.out.println("FAIL: " + fail.get());

        assertEquals(5, success.get(), "All cancellations should succeed");
        assertEquals(0, fail.get(), "No cancellation should fail");

        assertEquals(
                5,
                updatedEvent.getBookingPolicy().getAvailableCapacity(),
                "Capacity must be restored correctly for all bookings"
        );

        for (Booking b : bookings) {
            Booking updated = bookingRepository.findById(b.getId()).orElseThrow();
            assertEquals(Status.CANCELLED, updated.getStatus());
        }
    }
}