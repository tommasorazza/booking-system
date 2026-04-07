package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Event} entities.
 * Provides CRUD operations and custom queries for events.
 */
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Finds an event by its ID within a specific tenant.
     *
     * @param id the UUID of the event
     * @param tenant the tenant to which the event belongs
     * @return an Optional containing the Event if found, or empty if none exists
     */
    Optional<Event> findByIdAndTenant(UUID id, Tenant tenant);

    /**
     * Retrieves a paginated list of events for a given tenant.
     *
     * @param tenant the tenant whose events should be retrieved
     * @param pageable pagination and sorting information
     * @return a Page containing Event objects for the specified tenant
     */
    Page<Event> findByTenant(Tenant tenant, Pageable pageable);
}