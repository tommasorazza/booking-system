package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Venue;
import jakarta.annotation.Nullable;
import lombok.*;

import java.time.OffsetDateTime;
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

    /** Name of the user. */
    private String name;

    /** Birth date of the user. */
    private OffsetDateTime birthDate;

    /** Email address of the user. */
    private String email;

    /** Role of the user in the system (ADMIN, GUEST or PERFORMER). */
    private Role role;

    /** Venue the user belongs to. */
    private Venue venue;

    /** For performers, the weekly availability. Null for other roles. */
    @Nullable
    private AvailabilityDto availability;

    /** For performers, the performances they offer. Null for other roles. */
    @Nullable
    private Set<PerformanceDto> performances;
}