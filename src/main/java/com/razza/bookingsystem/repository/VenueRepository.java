package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Venue} entities.
 * Provides CRUD operations and custom queries for venues.
 */
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    /**
     * Finds a venue by its name.
     *
     * @param venueName the name of the venue to search for
     * @return an Optional containing the Venue if found, or empty if no venue exists with the given name
     */
    Optional<Venue> findByName(String venueName);
}