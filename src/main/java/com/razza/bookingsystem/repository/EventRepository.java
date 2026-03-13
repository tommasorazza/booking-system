package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository interface for {@link Event} entities.
 * Provides CRUD operations for events.
 */
public interface EventRepository extends JpaRepository<Event, UUID> {
    // Additional custom queries for events can be added here
}