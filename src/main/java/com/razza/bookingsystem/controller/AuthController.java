package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.dto.LoginRequest;
import com.razza.bookingsystem.dto.SignupRequest;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication operations such as signup and login.
 * Handles user registration and authentication based on JWT tokens.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user.
     *
     * @param signupRequest request containing name, birth date, email, password, role, venue name,
     *                      and optionally availability and performances (for performers)
     * @return the created user as a UserDto
     */
    @PostMapping("/signup")
    public UserDto signup(@Valid @RequestBody SignupRequest signupRequest) {
        return authService.signup(
                signupRequest.getName(),
                signupRequest.getBirthDate(),
                signupRequest.getEmail(),
                signupRequest.getPassword(),
                signupRequest.getRole(),
                signupRequest.getVenueName(),
                signupRequest.getAvailability(),
                signupRequest.getPerformances()
        );
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest request containing email, password, and venue name
     * @return a JWT token as a String
     */
    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword(),
                loginRequest.getVenueName()
        );
    }

}