package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Booking;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
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
     *
     * @param userId the ID of the user
     * @param eventId the ID of the event
     * @return an Optional containing the booking if found, or empty otherwise
     */
    Optional<Booking> findByUserIdAndEvent(UUID userId, Event event);    /**
     * Retrieves all bookings made by a specific user.
     *
     * @param userId the UUID of the user
     * @return list of bookings belonging to the user
     */

    List<Booking> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<Booking> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByEventIdAndStatus(UUID eventId, Status status);

    List<Booking> findAllByTenantId(UUID tenantId);

    UUID tenantId(UUID tenantId);

    Optional<Booking> findByEventId(UUID id);

    Optional<Booking> findByIdAndTenant(UUID bookingId, Tenant tenant);

    Collection<Booking> findByUserIdAndTenant(UUID userId, Tenant tenant);

    int countByEventIdAndStatus(UUID eventId, Status status);
}