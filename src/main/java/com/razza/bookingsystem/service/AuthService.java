package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.TenantRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for authentication-related operations.
 *
 * Handles:
 * - user registration (signup)
 * - user authentication (login)
 * - tenant resolution and creation
 *
 * Passwords are securely hashed before being stored.
 * JWT tokens are generated upon successful login.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Repository for user persistence.
     */
    private final UserRepository userRepository;

    /**
     * Mapper for converting User entities to DTOs.
     */
    private final UserMapper userMapper;

    /**
     * Encoder used to hash user passwords.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Service used to generate JWT tokens.
     */
    private final JwtService jwtService;

    /**
     * Authentication manager used to verify credentials during login.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Service for loading user-specific security details.
     */
    private final CustomUserDetailsService userDetailsService;

    /**
     * Repository for tenant persistence and lookup.
     */
    private final TenantRepository tenantRepository;

    /**
     * Registers a new user within a tenant.
     *
     * Behavior:
     * - checks if the email is already in use
     * - finds or creates a tenant with the given name
     * - hashes the user's password
     * - assigns a default USER role
     * - persists the user
     *
     * @param email the user's email, must be unique
     * @param password the user's raw password
     * @param tenantName the name of the tenant to associate with the user
     * @return the created user as a DTO
     * @throws RuntimeException if the email is already in use
     */
    public UserDto signup(String email, String password, String tenantName) {

        Tenant tenant;

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        Optional<Tenant> tenantOpt = tenantRepository.findByName(tenantName);

        if (tenantOpt.isPresent()) {
            tenant = tenantOpt.get();
        } else {
            tenant = Tenant.builder()
                    .name(tenantName)
                    .build();

            tenant = tenantRepository.save(tenant);
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(com.razza.bookingsystem.domain.Role.USER)
                .tenant(tenant)
                .build();

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * Behavior:
     * - validates credentials using AuthenticationManager
     * - loads user details
     * - generates a signed JWT token
     *
     * @param email the user's email
     * @param password the user's raw password
     * @return a JWT token if authentication is successful
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     */
    public String login(String email, String password) {

        authenticationManager.authenticate(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        email,
                        password
                )
        );

        var userDetails = userDetailsService.loadUserByUsername(email);

        return jwtService.generateToken(userDetails);
    }
}