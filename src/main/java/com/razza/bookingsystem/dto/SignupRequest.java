package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.Performance;
import com.razza.bookingsystem.domain.Role;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    private String name;

    @NotBlank
    private OffsetDateTime birthDate;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private Role role;

    @NotBlank
    private String venueName;

    @Nullable
    private String availability;

    @Nullable
    private Set<PerformanceDto> performances;
}