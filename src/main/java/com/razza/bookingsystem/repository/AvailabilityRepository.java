package com.razza.bookingsystem.repository;

import com.razza.bookingsystem.domain.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {

}
