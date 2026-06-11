package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest        // configures in memory database (H2), scans @Entity, creates repository beans, wraps each test in a transaction (that can be rolled back)
@ActiveProfiles("test")
class RepositoryQueryTest {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private UserRepository userRepository;

    @Autowired private EntityManager entityManager;

    private Venue venue;
    private User user;
    private Event event;

    @BeforeEach
    void setUp() {

        venue = venueRepository.saveAndFlush(
                Venue.builder()
                        .name("venue 1")
                        .build()
        );

        user = userRepository.saveAndFlush(
                User.builder()
                        .email("user@example.com")
                        .password("password")
                        .role(Role.GUEST)
                        .venue(venue)
                        .build()
        );

        event = eventRepository.saveAndFlush(
                Event.builder()
                        .name("Test Event")
                        .description("desc")
                        .location("loc")
                        .date(OffsetDateTime.now().plusDays(5))
                        .bookingPolicy(new BookingPolicy(10,10))
                        .venue(venue)
                        .status(Status.CONFIRMED)
                        .build()
        );
    }


    @Transactional
    @Test
    void decreaseCapacity_sufficientCapacity_updatesAndReturns1() {

        int affected = eventRepository.decreaseCapacity(event.getId(), 3);

        assertThat(affected).isEqualTo(1);

        entityManager.clear(); // this happens because decreaseCapacity performs a bulk JPQL update bypassing hibernate managed entities inside the cache, so DB must be hit instead of in-memory cache

        Event updated = eventRepository.findById(event.getId()).orElseThrow();

        assertThat(updated.getBookingPolicy().getAvailableCapacity()).isEqualTo(7);
    }

    @Transactional
    @Test
    void decreaseCapacity_exactlyAvailableCapacity_updatesAndReturns1() {

        int affected = eventRepository.decreaseCapacity(event.getId(), 10);

        assertThat(affected).isEqualTo(1);

        entityManager.clear();

        Event updated = eventRepository.findById(event.getId()).orElseThrow();

        assertThat(updated.getBookingPolicy().getAvailableCapacity()).isEqualTo(0);
    }

    @Transactional
    @Test
    void decreaseCapacity_insufficientCapacity_doesNotUpdateAndReturns0() {

        int affected = eventRepository.decreaseCapacity(event.getId(), 11);

        assertThat(affected).isEqualTo(0);

        entityManager.clear();

        Event updated = eventRepository.findById(event.getId()).orElseThrow();

        assertThat(updated.getBookingPolicy().getAvailableCapacity()).isEqualTo(10);
    }

    @Transactional
    @Test
    void decreaseCapacity_zeroAvailableCapacity_returns0() {

        eventRepository.decreaseCapacity(event.getId(), 10);

        int affected = eventRepository.decreaseCapacity(event.getId(), 1);

        assertThat(affected).isEqualTo(0);
    }


    @Transactional
    @Test
    void deleteBooking_confirmedBooking_setsStatusToCancelled() {

        Booking booking = bookingRepository.saveAndFlush(
                Booking.builder()
                        .user(user)
                        .event(event)
                        .venue(venue)
                        .quantity(1)
                        .status(Status.CONFIRMED)
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        bookingRepository.deleteBooking(booking.getId());

        entityManager.clear();

        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Transactional
    @Test
    void deleteBooking_alreadyCancelled_remainsCancelledWithoutException() {

        Booking booking = bookingRepository.saveAndFlush(
                Booking.builder()
                        .user(user)
                        .event(event)
                        .venue(venue)
                        .quantity(1)
                        .status(Status.CANCELLED)
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        bookingRepository.deleteBooking(booking.getId());

        entityManager.clear();

        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(Status.CANCELLED);
    }
}