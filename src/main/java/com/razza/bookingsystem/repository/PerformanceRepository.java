package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PerformanceRepository extends JpaRepository<Performance, UUID> {

    Optional<Performance> findById(UUID performanceId);
}





















