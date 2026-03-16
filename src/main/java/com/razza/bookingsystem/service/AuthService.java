package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for authentication-related operations.
 * Handles user signup and password hashing.
 * Login is handled by JWT filters and security configuration.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder; // for hashing passwords
    private final JwtService jwtService;

    /**
     * Registers a new user with a default role.
     * Hashes the password before persisting.
     *
     * @param email the user's email (must be unique)
     * @param password the user's raw password
     * @return the newly created user as a DTO
     * @throws RuntimeException if the email is already in use
     */
    public UserDto signup(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(com.razza.bookingsystem.domain.Role.USER) // default role
                // tenantId can be assigned here if multi-tenant logic applies
                .build();

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    public String login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return jwtService.generateToken(user.getEmail());
    }
}