package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for {@link Availability} entities.
 * Provides the standard CRUD operations through Spring Data JPA.
 */
public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {

}