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

/**
 * Controller for managing users.
 * Provides endpoints for promoting and deleting users, retrieving performers,
 * and letting performers update their own availability.
 */
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

    /**
     * Retrieves the performers of the authenticated user's venue that are available on a given date.
     *
     * Access is restricted to users with ADMIN role.
     *
     * @param date the date to check availability for (optional)
     * @param user the currently authenticated admin
     * @return list of available performers as UserDto
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/performers")
    public List<UserDto> getAvailablePerformersByDate(@RequestParam(required = false) LocalDate date, @AuthenticationPrincipal CustomUserDetails user){
        Venue venue = user.getVenue();
        return userService.getAvailablePerformersByDate(date, venue.getId());
    }

    /**
     * Retrieves a single performer of the authenticated user's venue by their ID.
     *
     * Access is restricted to users with ADMIN role.
     *
     * @param performerId the ID of the performer
     * @param user the currently authenticated admin
     * @return the performer as a UserDto
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/performers/{performerId}")
    public UserDto getPerformer(@PathVariable UUID performerId, @AuthenticationPrincipal CustomUserDetails user){
        Venue venue = user.getVenue();
        return userService.getPerformer(performerId, venue, Role.PERFORMER);
    }

    /**
     * Updates the availability of the authenticated performer.
     *
     * Access is restricted to users with PERFORMER role.
     *
     * @param availability the new availability as a String
     * @param user the currently authenticated performer
     */
    @PreAuthorize("hasRole('PERFORMER')")
    @PutMapping("/me/availability")
    public void modifyAvailability(@RequestBody String availability, @AuthenticationPrincipal CustomUserDetails user){
        userService.modifyAvailability(availability, user.getEmail(), user.getVenue());
    }

}