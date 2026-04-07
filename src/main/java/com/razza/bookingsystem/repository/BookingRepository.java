package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Booking;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Booking} entities.
 * Provides CRUD operations and custom queries for bookings.
 */
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Finds a booking for a specific user and event.
     * Enforces the rule that a user can have at most one booking per event.
     *
     * @param userId the UUID of the user
     * @param event the Event object
     * @return an Optional containing the Booking if found, or empty if none exists
     */
    Optional<Booking> findByUserIdAndEvent(UUID userId, Event event);

    /**
     * Finds a booking by its ID within a specific tenant.
     *
     * @param bookingId the UUID of the booking
     * @param tenant the tenant to which the booking belongs
     * @return an Optional containing the Booking if found, or empty otherwise
     */
    Optional<Booking> findByIdAndTenant(UUID bookingId, Tenant tenant);

    /**
     * Retrieves all bookings made by a specific user within a given tenant.
     *
     * @param userId the UUID of the user
     * @param tenant the tenant to which the bookings belong
     * @return a collection of bookings for the given user and tenant
     */
    Collection<Booking> findByUserIdAndTenant(UUID userId, Tenant tenant);

    /**
     * Counts the number of bookings for a given event with a specific status.
     *
     * @param eventId the UUID of the event
     * @param status the booking status to filter by
     * @return the number of bookings to that event matching the status 
     */
    int countByEventIdAndStatus(UUID eventId, Status status);
}