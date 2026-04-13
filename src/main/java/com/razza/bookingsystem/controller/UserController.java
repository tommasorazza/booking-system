package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Promotes a user to ADMIN role within the authenticated user's tenant.
     *
     * Access is restricted to users with ADMIN role.
     *
     * @param userId the ID of the user to promote
     * @param user the currently authenticated user
     * @return the updated user as a UserDto
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}")
    public UserDto makeAdmin(@PathVariable UUID userId, @AuthenticationPrincipal CustomUserDetails user) {
        Tenant tenant = user.getTenant();
        return userService.makeAdmin(userId, tenant);
    }

    /**
     * Deletes a user.
     *
     * Access is restricted to users with ADMIN role.
     *
     * The user can only be deleted if they have no active bookings
     * or all their bookings are already canceled.
     *
     * @param userId the ID of the user to delete
     * @param user the currently authenticated admin
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable UUID userId, @AuthenticationPrincipal CustomUserDetails user) {
        Tenant tenant = user.getTenant();
        userService.deleteUser(userId, tenant);
    }
}