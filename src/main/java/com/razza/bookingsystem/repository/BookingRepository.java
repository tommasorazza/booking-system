package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Booking} entities.
 * Provides CRUD operations and custom queries for bookings.
 */
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Finds a booking for a specific user, event, and status.
     *
     * @param user the User object
     * @param event the Event object
     * @param status the booking status to filter by
     * @return an Optional containing the Booking if found, or empty if none exists
     */
    Optional<Booking> findByUserAndEventAndStatus(User user, Event event, Status status);

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
     * @return the number of bookings for that event matching the status
     */
    @Query("""
    SELECT COUNT(b)
    FROM Booking b
    WHERE b.event.id = :eventId AND b.status = :status
    """)
    int countByEventIdAndStatus(UUID eventId, Status status);

    /**
     * Counts the number of bookings for a given user with a specific status.
     *
     * @param userId the UUID of the user
     * @param status the booking status to filter by
     * @return the number of bookings for that user matching the status
     */
    int countByUserIdAndStatus(UUID userId, Status status);

    /**
     * Retrieves all bookings associated with a given event.
     *
     * @param id the ID of the event
     * @return a collection of bookings for the specified event
     */
    Collection<Booking> findByEventId(UUID id);

    /**
     * Marks a booking as CANCELLED if it is not already canceled.
     *
     * @param id the UUID of the booking to cancel
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
    UPDATE Booking b
    SET b.status = 'CANCELLED'
    WHERE b.id = :id AND b.status != 'CANCELLED'
    """)
    void deleteBooking(UUID id);
}