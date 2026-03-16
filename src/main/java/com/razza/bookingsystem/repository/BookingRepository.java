package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

/**
 * Repository interface for {@link Booking} entities.
 * Provides CRUD operations and custom queries for bookings.
 */
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Finds a booking by a specific user for a specific event.
     * Enforces the rule that a user can book a given event only once.
     *
     * @param userId the ID of the user
     * @param eventId the ID of the event
     * @return an Optional containing the booking if found, or empty otherwise
     */
    Optional<Booking> findByUserIdAndEventId(UUID userId, UUID eventId);

    /**
     * Retrieves all bookings made by a specific user.
     *
     * @param userId the UUID of the user
     * @return list of bookings belonging to the user
     */
    List<Booking> findByUserId(UUID userId);
}