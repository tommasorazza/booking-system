package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.PerformanceType;
import com.razza.bookingsystem.dto.PerformanceDto;
import com.razza.bookingsystem.dto.ScheduledPerformanceDto;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.PerformanceService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing performances.
 * Provides endpoints for retrieving, adding, modifying, and deleting performances.
 */
@RestController
@RequestMapping("/performances")
@AllArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    /**
     * Retrieves the performances of a user, together with their scheduled slots.
     *
     * If the authenticated user is an admin, the performances of another user in the venue
     * can be retrieved. Otherwise, only the authenticated user's performances are returned.
     *
     * @param userId optional ID of the user whose performances are requested (used only by admins)
     * @param user the authenticated user details
     * @return list of ScheduledPerformanceDto
     */
    @PreAuthorize("hasAnyRole('PERFORMER', 'ADMIN')")
    @GetMapping
    public List<ScheduledPerformanceDto> getPerformances(@RequestParam(required = false) UUID userId, @AuthenticationPrincipal CustomUserDetails user){

        return performanceService.getPerformances(userId, user.getId(), isAdmin(user), user.getVenue());
    }

    /**
     * Adds a new performance.
     *
     * If the authenticated user is an admin, the performance can be added on behalf
     * of another user. Otherwise, it is added to the authenticated user.
     *
     * @param performanceDto request containing the performance details
     * @param userId optional ID of the user the performance is added to (used only by admins)
     * @param user the authenticated user details
     */
    @PreAuthorize("hasAnyRole('PERFORMER', 'ADMIN')")
    @PostMapping
    public void addPerformance(@Valid @RequestBody PerformanceDto performanceDto, @RequestParam(required = false) UUID userId, @AuthenticationPrincipal CustomUserDetails user){
        performanceService.addPerformance(performanceDto, userId, user.getId(), isAdmin(user), user.getVenue());
    }

    /**
     * Deletes a performance by its ID.
     *
     * @param performanceId the ID of the performance to delete
     */
    @PreAuthorize("hasAnyRole('PERFORMER', 'ADMIN')")
    @DeleteMapping
    public void deletePerformance(@RequestParam UUID performanceId){
        performanceService.deletePerformance(performanceId);
    }

    /**
     * Updates the type and duration of an existing performance.
     *
     * @param performanceId the ID of the performance to update
     * @param performanceType the new performance type
     * @param duration the new duration of the performance
     */
    @PreAuthorize("hasAnyRole('PERFORMER', 'ADMIN')")
    @PutMapping("/{performanceId}")
    public void modifyPerformance(@PathVariable UUID performanceId, @RequestParam PerformanceType performanceType, @RequestParam int duration){
        performanceService.modifyPerformance(performanceId, performanceType, duration);
    }

    /**
     * Checks whether the authenticated user has admin privileges.
     *
     * @param user the authenticated user details
     * @return true if the user has ROLE_ADMIN, false otherwise
     */
    private boolean isAdmin(CustomUserDetails user) {
        return user.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}