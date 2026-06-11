package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Venue;
import com.razza.bookingsystem.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.razza.bookingsystem.domain.Role.PERFORMER;

/**
 * Repository interface for {@link User} entities.
 * Provides CRUD operations and custom query methods for users.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address within a specific venue.
     *
     * @param email the email address of the user
     * @param venue the venue to which the user belongs
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    Optional<User> findByEmailAndVenue(String email, Venue venue);

    /**
     * Finds a user by their ID within a specific venue.
     *
     * @param id the unique identifier of the user
     * @param venueId the unique identifier of the venue
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    Optional<User> findByIdAndVenueId(UUID id, UUID venueId);

    List<User> findByRoleAndVenueId(Role role, UUID venueId);

    Optional<User> findByEmailAndVenueAndRole(String email, Venue venue, Role role);

    Optional<User> findByIdAndVenueAndRole(UUID id, Venue venue, Role role);
}