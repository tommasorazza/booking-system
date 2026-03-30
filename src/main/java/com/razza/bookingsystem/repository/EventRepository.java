package com.razza.bookingsystem.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Event} entities.
 * Provides CRUD operations for events.
 */
public interface EventRepository extends JpaRepository<Event, UUID> {
    // Additional custom queries for events can be added here

    Optional<Event> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Event> findAllByTenantId(UUID tenantId);

    UUID tenantId(UUID tenantId);

    Page<Event> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Event> findByIdAndTenant(UUID id, Tenant tenant);

    Page<Event> findByTenant(Tenant tenant, Pageable pageable);
}