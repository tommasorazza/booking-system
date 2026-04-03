package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.dto.LoginRequest;
import com.razza.bookingsystem.dto.SignupRequest;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.AuthService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for authentication-related operations such as signup and login.
 * Handles JWT token generation and user registration.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user with default tenant assignment.
     *
     * @param userDto the user registration details
     * @return the registered user's data as UserDto
     */
    @PostMapping("/signup")
    public UserDto signup(@RequestBody SignupRequest signupRequest) {
        return authService.signup(signupRequest.getEmail(), signupRequest.getPassword(), signupRequest.getTenantName());
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param email the user's email
     * @param password the user's password
     * @return JWT token as a String
     */
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest.getEmail(), loginRequest.getPassword(), loginRequest.getTenantName());
    }

    /**
     * Promotes a user to ADMIN role within the tenant of the authenticated user.
     *
     * Access restricted to users with ADMIN role.
     *
     * @param userId the UUID of the user to promote
     * @param authentication the current authenticated principal
     * @return the updated user as a DTO
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("users/{userId}")
    public UserDto makeAdmin(@PathVariable UUID userId, Authentication authentication) {
        CustomUserDetails user = getUser(authentication);
        Tenant tenant = user.getTenant();
        return authService.makeAdmin(userId, tenant);
    }

    /**
     * Extracts the CustomUserDetails object from the Authentication.
     *
     * This method assumes that the principal stored in the Authentication
     * is an instance of CustomUserDetails.
     *
     * @param auth the Authentication object
     * @return the authenticated user as CustomUserDetails
     * @throws ClassCastException if the principal is not of type CustomUserDetails
     */
    private CustomUserDetails getUser(org.springframework.security.core.Authentication auth) {
        return (CustomUserDetails) auth.getPrincipal();
    }


}