package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.*;
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
     * @param user the User object  
     * @param event the Event object
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
     * @return the number of bookings to that event matching the status 
     */
    int countByEventIdAndStatus(UUID eventId, Status status);

    /**
     * Counts the number of bookings for a given user with a specific status.
     *
     * @param userId the UUID of the user
     * @param status the booking status to filter by
     * @return the number of bookings to that user matching the status
     */
    int countByUserIdAndStatus(UUID userId, Status status);

    /**
     * Retrieves all bookings associated with a given event.
     *
     * @param id the ID of the event
     * @return a collection of bookings for the specified event
     */
    Collection<Booking> findByEventId(UUID id);}