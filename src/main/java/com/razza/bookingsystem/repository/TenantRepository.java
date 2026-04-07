package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Tenant} entities.
 * Provides CRUD operations and custom queries for tenants.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Finds a tenant by its name.
     *
     * @param tenantName the name of the tenant to search for
     * @return an Optional containing the Tenant if found, or empty if no tenant exists with the given name
     */
    Optional<Tenant> findByName(String tenantName);
}