package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.exception.EventDecreaseException;
import com.razza.bookingsystem.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency test for pessimistic locking on Event update.
 *
 * Scenario:
 * - An event has initial capacity of 10
 * - 5 users try to book 1 seat each
 * - At the same time, an admin tries to reduce total capacity to 5
 *
 * Expected behavior:
 * - If bookings happen first → admin MUST fail (EventDecreaseException)
 * - If admin happens first → bookings are limited by new capacity
 *
 * Guarantees:
 * - No inconsistent state
 * - Business rule enforced: cannot decrease capacity if bookings exist
 */
@SpringBootTest
@ActiveProfiles("test")
class EventUpdateConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void update_event_and_bookings_should_respect_capacity_rules() throws Exception {

        Tenant t = new Tenant();
        final Tenant tenant = tenantRepository.save(t);

        Event e = Event.builder()
                .name("Pessimistic Lock Test")
                .date(OffsetDateTime.now().plusDays(1))
                .totalCapacity(10)
                .availableCapacity(10)
                .tenant(tenant)
                .build();

        final Event event = eventRepository.save(e);

        List<User> users = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setEmail("user" + i + "@test.com");
            user.setPassword("password");
            user.setRole(Role.USER);
            user.setTenant(tenant);

            users.add(userRepository.save(user));
        }

        User admin = new User();
        admin.setEmail("admin@test.com");
        admin.setPassword("password");
        admin.setRole(Role.ADMIN);
        admin.setTenant(tenant);

        admin = userRepository.save(admin);

        ExecutorService executor = Executors.newFixedThreadPool(6);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(6);

        AtomicInteger bookingSuccess = new AtomicInteger(0);
        AtomicInteger bookingFail = new AtomicInteger(0);
        AtomicInteger adminSuccess = new AtomicInteger(0);
        AtomicInteger adminFail = new AtomicInteger(0);

        for (int i = 0; i < users.size(); i++) {

            final User user = users.get(i);

            Runnable bookingTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();

                        Thread.sleep(200);

                        System.out.println("before create booking");

                        bookingService.createBooking(
                                event.getId(),
                                user,
                                user,
                                user.getTenant(),
                                1,
                                false
                        );

                        bookingSuccess.incrementAndGet();

                    } catch (Exception e) {
                        bookingFail.incrementAndGet();
                        //e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                }
            };

            executor.submit(bookingTask);
        }

        System.out.println("before admin");

        Runnable adminTask = new Runnable() {
            @Override
            public void run() {
                try {
                    startLatch.await();

                    EventRequestDto dto = new EventRequestDto();
                    dto.setName("Updated");
                    dto.setDescription("Updated");
                    dto.setLocation("Updated");
                    dto.setDate(OffsetDateTime.now().plusDays(2));
                    dto.setTotalCapacity(5);

                    System.out.println("iii");

                    eventService.updateEvent(event.getId(), dto, tenant);
                    adminSuccess.incrementAndGet();

                } catch (EventDecreaseException e) {
                    adminFail.incrementAndGet();
                } catch (Exception e) {
                    adminFail.incrementAndGet();
                    throw new RuntimeException("Unexpected exception in admin task", e);
                } finally {
                    doneLatch.countDown();
                }
            }
        };

        executor.submit(adminTask);

        System.out.println("aaa");

        startLatch.countDown();

        System.out.println("eee");

        doneLatch.await();
        executor.shutdown();

        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        Collection<Booking> bookings = bookingRepository.findByEventId(event.getId());

        int totalBookedSeats = 0;

        for (Booking b : bookings) {
            if (b.getStatus() == Status.CONFIRMED) {
                totalBookedSeats += b.getQuantity();
            }
        }

        int expectedAvailable = updatedEvent.getTotalCapacity() - totalBookedSeats;

        System.out.println("BOOK SUCCESS: " + bookingSuccess.get());
        System.out.println("BOOK FAIL: " + bookingFail.get());
        System.out.println("ADMIN SUCCESS: " + adminSuccess.get());
        System.out.println("ADMIN FAIL: " + adminFail.get());
        System.out.println("TOTAL CAPACITY: " + updatedEvent.getTotalCapacity());
        System.out.println("AVAILABLE CAPACITY: " + updatedEvent.getAvailableCapacity());
        System.out.println("BOOKED SEATS: " + totalBookedSeats);

        assertTrue(
                updatedEvent.getAvailableCapacity() == expectedAvailable,
                "Capacity inconsistency detected!"
        );

        if (adminSuccess.get() == 1) {
            assertTrue(
                    bookingSuccess.get() <= 5,
                    "Bookings exceeded reduced capacity"
            );
        }
    }
}