package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service responsible for managing User entities.
 * Handles user creation, retrieval, role updates, and paginated listing.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new user with the specified email, password, role, and tenant.
     *
     * @param email the user's email (must be unique)
     * @param rawPassword the user's raw password (will be encoded)
     * @param role the role assigned to the user
     * @param tenantId the tenant ID to which the user belongs
     * @return the saved user as a DTO
     * @throws RuntimeException if the email is already in use
     */
    public UserDto createUser(String email, String rawPassword, Role role, UUID tenantId) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .tenantId(tenantId)
                .build();

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    /**
     * Retrieves a user by their unique ID.
     *
     * @param id the UUID of the user
     * @return the corresponding UserDto
     * @throws RuntimeException if the user is not found
     */
    public UserDto getUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toDto(user);
    }

    /**
     * Retrieves all users with pagination support.
     *
     * @param pageable pagination and sorting information
     * @return a page of UserDto objects
     */
    public Page<UserDto> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toDto);
    }

    /**
     * Updates a user's role.
     * Typically restricted to admin users.
     *
     * @param userId the UUID of the user to update
     * @param newRole the new role to assign
     * @return the updated UserDto
     * @throws RuntimeException if the user is not found
     */
    public UserDto updateRole(UUID userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(newRole);
        User updated = userRepository.save(user);
        return userMapper.toDto(updated);
    }
}