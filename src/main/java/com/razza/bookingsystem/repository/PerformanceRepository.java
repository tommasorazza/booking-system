package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Performance} entities.
 * Provides the standard CRUD operations through Spring Data JPA.
 */
public interface PerformanceRepository extends JpaRepository<Performance, UUID> {

    /**
     * Finds a performance by its ID.
     *
     * @param performanceId the UUID of the performance
     * @return an Optional containing the Performance if found, or empty otherwise
     */
    Optional<Performance> findById(UUID performanceId);
}