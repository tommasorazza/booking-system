package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.Performance;
import com.razza.bookingsystem.dto.LoginRequest;
import com.razza.bookingsystem.dto.SignupRequest;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.AuthService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
     * @param signupRequest request containing email, password, and venue name
     * @return the created user as a UserDto
     */
    @PostMapping("/signup")
    public UserDto signup(@RequestBody SignupRequest signupRequest) {
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
    public String login(@RequestBody LoginRequest loginRequest) {
        return authService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword(),
                loginRequest.getVenueName()
        );
    }

}