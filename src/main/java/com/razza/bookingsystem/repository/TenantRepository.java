package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository interface for {@link Tenant} entities.
 * Provides CRUD operations and custom queries for tenants.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

}