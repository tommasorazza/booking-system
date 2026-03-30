package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.dto.LoginRequest;
import com.razza.bookingsystem.dto.SignupRequest;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
        return authService.login(loginRequest.getEmail(), loginRequest.getPassword());
    }
}