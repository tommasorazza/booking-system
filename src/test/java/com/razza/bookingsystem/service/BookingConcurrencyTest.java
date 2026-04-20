package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.TenantRepository;
import com.razza.bookingsystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test that verifies the booking system is thread-safe
 * when multiple users attempt to book the last available seats.
 *
 * Scenario:
 * - An event has only 1 seat available.
 * - 10 users attempt to book 1 seat concurrently.
 *
 * Expected outcome:
 * - Exactly 1 booking succeeds.
 * - The remaining 9 attempts fail due to insufficient capacity.
 *
 * This test ensures that the booking logic correctly handles
 * concurrent access and prevents overbooking.
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;
    
    @Test
    void only_one_booking_should_succeed_when_one_seat_left() throws Exception {

        Tenant tenant = new Tenant();

        tenantRepository.save(tenant);

        Event event = Event.builder()
                .name("Concurrency Test Event")
                .date(OffsetDateTime.now().plusDays(1))
                .availableCapacity(1)
                .totalCapacity(1)
                .tenant(tenant)
                .build();

        eventRepository.save(event);

        List<User> users = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setTenant(tenant);
            userRepository.save(user);
            users.add(user);
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);

        CountDownLatch startLatch = new CountDownLatch(1);

        CountDownLatch doneLatch = new CountDownLatch(10);

        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();

        for (User user : users) {

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();

                        bookingService.createBooking(
                                event.getId(),
                                user,
                                user,
                                tenant,
                                1,
                                false
                        );

                        results.add("SUCCESS");

                    } catch (Exception e) {
                        results.add("FAIL: " + e.getClass().getSimpleName());

                    } finally {
                        doneLatch.countDown();
                    }
                }
            };

            executor.submit(task);
        }

        startLatch.countDown();

        doneLatch.await();

        int successCount = 0;
        int failCount = 0;

        for (String result : results) {
            if ("SUCCESS".equals(result)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        System.out.println("SUCCESS: " + successCount);
        System.out.println("FAIL: " + failCount);

        assertEquals(1, successCount);
        assertEquals(9, failCount);

        executor.shutdown();
    }
}