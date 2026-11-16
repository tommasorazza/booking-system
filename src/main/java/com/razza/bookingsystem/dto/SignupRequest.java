package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.Role;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * DTO used for user registration requests.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    /** Name of the user. */
    @NotBlank
    private String name;

    /** Birth date of the user. */
    @NotNull
    private OffsetDateTime birthDate;

    /** Email address of the user. */
    @Email
    @NotBlank
    private String email;

    /** Password of the user, in plain text as sent by the client. */
    @NotBlank
    private String password;

    /** Role of the user (ADMIN, GUEST or PERFORMER). */
    @NotNull
    private Role role;

    /** Name of the venue the user signs up to. */
    @NotBlank
    private String venueName;

    /** For performers, the weekly availability. Null for other roles. */
    @Nullable
    private String availability;

    /** For performers, the performances they offer. Null for other roles. */
    @Valid
    @Nullable
    private Set<PerformanceDto> performances;
}