package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.exception.UserDeleteException;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UserMapper userMapper;

    /**
     * Promotes an existing user to ADMIN within a specific tenant.
     *
     * The method:
     * - retrieves the user scoped to the given tenant
     * - updates the role to ADMIN
     * - persists the updated user entity
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

    /**
     * Deletes a user if they have no active confirmed bookings.
     *
     * Rules:
     * - user must exist within the given tenant
     * - user cannot have active CONFIRMED bookings
     *
     * @param userId identifier of the user to delete
     * @param tenant tenant to which the user belongs
     *
     * @throws ResourceNotFoundException if the user is not found in the tenant
     * @throws UserDeleteException if the user has active confirmed bookings
     */
    public void deleteUser(UUID userId, Tenant tenant){
        User user = userRepository.findByIdAndTenantId(userId, tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));

        int activeBookings = bookingRepository.countByUserIdAndStatus(userId, Status.CONFIRMED);

        if (activeBookings > 0){
            throw new UserDeleteException(activeBookings);
        }

        userRepository.delete(user);
    }
}