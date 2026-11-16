package com.razza.bookingsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO used for user login requests.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /** Email address of the user. */
    @Email
    @NotBlank
    private String email;

    /** Password of the user, in plain text as sent by the client. */
    @NotBlank
    private String password;

    /** Name of the venue the user belongs to. */
    @NotBlank
    private String venueName;
}