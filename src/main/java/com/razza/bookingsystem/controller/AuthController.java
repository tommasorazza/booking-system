package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.dto.LoginRequest;
import com.razza.bookingsystem.dto.SignupRequest;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.AuthService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * Controller for authentication-related operations such as signup and login.
 * Handles user registration and JWT-based authentication.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user.
     *
     * @param signupRequest request containing email, password, and tenant name
     * @return the created user as a UserDto
     */
    @PostMapping("/signup")
    public UserDto signup(@RequestBody SignupRequest signupRequest) {
        return authService.signup(
                signupRequest.getEmail(),
                signupRequest.getPassword(),
                signupRequest.getTenantName()
        );
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest request containing email, password, and tenant name
     * @return a JWT token as a String
     */
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest loginRequest) {
        return authService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword(),
                loginRequest.getTenantName()
        );
    }

    /**
     * Extracts the CustomUserDetails from the Authentication object.
     *
     * @param auth the Authentication object
     * @return the authenticated user details
     */
    private CustomUserDetails getUser(Authentication auth) {
        return (CustomUserDetails) auth.getPrincipal();
    }
}