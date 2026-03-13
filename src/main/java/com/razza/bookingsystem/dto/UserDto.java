package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.Role;
import lombok.*;
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

    /** Email address of the user. */
    private String email;

    /** Role of the user in the system (e.g., ADMIN, USER). */
    private Role role;
}