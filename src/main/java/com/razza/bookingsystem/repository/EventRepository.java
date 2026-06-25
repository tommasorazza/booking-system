package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Venue;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static com.razza.bookingsystem.domain.Status.CANCELLED;

/**
 * Repository interface for {@link Event} entities.
 * Provides CRUD operations and custom queries for events.
 */
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Retrieves a paginated list of events for a given venue.
     *
     * @param venue the venue whose events should be retrieved
     * @return a Page containing Event objects for the specified venue
     */
    Page<Event> findByVenue(Venue venue, Pageable page);

    Collection<Event> findByVenue(Venue venue);

    /**
     * Atomically decreases the available capacity of a event.
     *
     * This operation is performed as a single database update to ensure
     * thread safety under concurrent booking requests.
     * The update only succeeds if the event has sufficient available capacity.
     * If the available capacity is less than the requested quantity, no rows are updated.
     *
     * @param id the ID of the event whose capacity should be decreased
     * @param quantity the number of seats to subtract from available capacity
     * @return the number of rows affected (1 if successful, 0 if not enough capacity)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Transactional
    @Query(value = """
    UPDATE Event e
    SET e.bookingPolicy.availableCapacity = e.bookingPolicy.availableCapacity - :quantity
    WHERE e.id = :id AND e.bookingPolicy.availableCapacity >= :quantity
    """)
    int decreaseCapacity(UUID id, int quantity);

    /**
     * Atomically increases the available capacity of a event.
     *
     * This operation is performed as a single database update to ensure
     * thread safety under concurrent booking requests.
     *
     * @param id the ID of the event whose capacity should be increased
     * @param quantity the number of seats to add to available capacity
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
    UPDATE Event e
    SET e.bookingPolicy.availableCapacity = e.bookingPolicy.availableCapacity + :quantity
    WHERE e.id = :id
    """)
    void increaseCapacity(UUID id, int quantity);

    /**
     * Finds a event by its ID within a specific venue, acquiring a pessimistic write lock.
     *
     * The pessimistic lock prevents concurrent modifications to the event row
     * for the duration of the transaction, ensuring consistency under concurrent booking requests.
     *
     * @param id the UUID of the event
     * @param venue the venue to which the event belongs
     * @return an Optional containing the Event if found, or empty if none exists
     */
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT e
    FROM Event e
    WHERE e.id = :id AND e.venue = :venue
    """)
    Optional<Event> findByIdAndVenue(UUID id, Venue venue);

    @Transactional
    @Query("""
    SELECT e
    FROM Event e
    WHERE e.date BETWEEN :start AND :end
    AND e.venue = :venue
    AND e.status = :status
    """)
    List<Event> findByDateAndVenueAndStatus(OffsetDateTime start, OffsetDateTime end, Venue venue, Status status);
}