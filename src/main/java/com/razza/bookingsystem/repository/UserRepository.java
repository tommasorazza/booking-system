package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link User} entities.
 * Provides CRUD operations and custom queries for users.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     * Useful for authentication or login operations.
     *
     * @param email the email of the user
     * @return an Optional containing the user if found, or empty otherwise
     */
    Optional<User> findByEmail(String email);
}