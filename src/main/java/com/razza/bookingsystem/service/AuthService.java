package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.exception.EmailAlreadyExistsException;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.exception.UserAlreadyExistsException;
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
 * - user registration
 * - user authentication
 * - tenant resolution and creation
 *
 * Passwords are hashed before being stored.
 * JWT tokens are generated upon successful authentication.
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
     * - hashes the password
     * - assigns the USER role
     * - persists the user
     *
     * @param email unique email of the user
     * @param password raw password of the user
     * @param tenantName name of the tenant
     * @return created user as a DTO
     * @throws RuntimeException if the email is already in use
     */
    public UserDto signup(String email, String password, String tenantName) {

        Tenant tenant;

        Optional<Tenant> tenantOpt = tenantRepository.findByName(tenantName);

        if (tenantOpt.isPresent()) {
            tenant = tenantOpt.get();
        } else {
            tenant = Tenant.builder()
                    .name(tenantName)
                    .build();

            tenant = tenantRepository.save(tenant);
        }

        if (userRepository.findByEmailAndTenant(email, tenant).isPresent()) {
            throw new UserAlreadyExistsException(email);
        }

        User user = User.builder()
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
     * - validates credentials using the authentication manager
     * - loads user details
     * - generates a signed JWT token
     *
     * @param email user email
     * @param password raw password
     * @return JWT token if authentication succeeds
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

    /**
     * Promotes an existing user to ADMIN within a specific tenant.
     *
     * Behavior:
     * - retrieves the user scoped to the given tenant
     * - updates the role to ADMIN
     * - persists the updated user
     *
     * @param userId identifier of the user to promote
     * @param tenant tenant to which the user belongs
     * @return updated user as a DTO
     * @throws RuntimeException if the user is not found in the given tenant
     */
    public UserDto makeAdmin(UUID userId, Tenant tenant){

        User user = userRepository.findByIdAndTenantId(userId, tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));

        user.setRole(Role.ADMIN);

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }
}