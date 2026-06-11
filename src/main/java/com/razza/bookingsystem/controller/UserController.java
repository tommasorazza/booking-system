package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.PerformanceDto;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    /**
     * Promotes a user to ADMIN role within the authenticated user's venue.
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
        Venue venue = user.getVenue();
        return userService.makeAdmin(userId, venue);
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
        Venue venue = user.getVenue();
        userService.deleteUser(userId, venue);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/performers")
    public List<UserDto> getAvailablePerformersByDate(@RequestParam(required = false) LocalDate date, @AuthenticationPrincipal CustomUserDetails user){
        Venue venue = user.getVenue();
        return userService.getAvailablePerformersByDate(date, venue.getId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/performers/{performerId}")
    public UserDto getPerformer(@PathVariable UUID performerId, @AuthenticationPrincipal CustomUserDetails user){
        Venue venue = user.getVenue();
        return userService.getPerformer(performerId, venue, Role.PERFORMER);
    }

    @PreAuthorize("hasRole('PERFORMER')")
    @PutMapping("/me/availability")
    public void modifyAvailability(@RequestBody String availability, @AuthenticationPrincipal CustomUserDetails user){
        userService.modifyAvailability(availability, user);
    }

}