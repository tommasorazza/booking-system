package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link User} entities.
 * Provides CRUD operations and custom query methods for users.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address within a specific tenant.
     *
     * @param email the email address of the user
     * @param tenant the tenant to which the user belongs
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    Optional<User> findByEmailAndTenant(String email, Tenant tenant);

    /**
     * Finds a user by their ID within a specific tenant.
     *
     * @param id the unique identifier of the user
     * @param tenantId the unique identifier of the tenant
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
}