package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.exception.UserAlreadyExistsException;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.TenantRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
 * JWT tokens are generated after successful authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final TenantRepository tenantRepository;

    /**
     * Registers a new user within a tenant.
     *
     * Behavior:
     * - checks if the email is already in use within the tenant
     * - retrieves a tenant with the given name
     * - hashes the password
     * - assigns the USER role
     * - persists the user
     *
     * @param email unique email of the user within the tenant
     * @param password raw password of the user
     * @param tenantName name of the tenant
     * @return created user as a DTO
     *
     * @throws UserAlreadyExistsException if a user with the same email already exists within the tenant
     */
    public UserDto signup(String email, String password, String tenantName) {

        Tenant tenant = tenantRepository.findByName(tenantName)
                .orElseThrow(() -> new ResourceNotFoundException("tenant"));

        if (userRepository.findByEmailAndTenant(email, tenant).isPresent()) {
            throw new UserAlreadyExistsException(email);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .tenant(tenant)
                .build();

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * Behavior:
     * - builds a tenant-scoped username (email + tenant)
     * - validates credentials using the authentication manager
     * - extracts authenticated user details
     * - generates a signed JWT token
     *
     * @param email user email
     * @param password raw password
     * @param tenantName tenant name used for scoping authentication
     * @return JWT token if authentication succeeds
     *
     * @throws AuthenticationException if credentials are invalid
     */
    public String login(String email, String password, String tenantName) {

        String username = email + "|" + tenantName;

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

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
     *
     * @throws ResourceNotFoundException if the user is not found in the given tenant
     */
    public UserDto makeAdmin(UUID userId, Tenant tenant) {

        User user = userRepository.findByIdAndTenantId(userId, tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));

        user.setRole(Role.ADMIN);

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }
}