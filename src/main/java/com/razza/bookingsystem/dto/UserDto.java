package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.Availability;
import com.razza.bookingsystem.domain.Performance;
import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Venue;
import jakarta.annotation.Nullable;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object representing a user.
 * Used to transfer user details between layers without exposing sensitive fields like passwords.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    /** Unique identifier for the user. */
    private UUID id;

    private String name;

    private OffsetDateTime birthDate;

    /** Email address of the user. */
    private String email;

    /** Role of the user in the system (ADMIN, GUEST). */
    private Role role;

    private Venue venue;

    @Nullable
    private AvailabilityDto availability;

    @Nullable
    private Set<PerformanceDto> performances;
}