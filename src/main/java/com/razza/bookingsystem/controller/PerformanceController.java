package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Performance;
import com.razza.bookingsystem.domain.PerformanceType;
import com.razza.bookingsystem.domain.Venue;
import com.razza.bookingsystem.dto.PerformanceDto;
import com.razza.bookingsystem.dto.ScheduledPerformanceDto;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.PerformanceService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/performances")
@AllArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    @PreAuthorize("hasAnyRole('PERFORMER', 'ADMIN')")
    @GetMapping
    public List<ScheduledPerformanceDto> getPerformances(@RequestParam(required = false) UUID userId, @AuthenticationPrincipal CustomUserDetails user){

        return performanceService.getPerformances(userId, user.getId(), isAdmin(user), user.getVenue());
    }

    @PreAuthorize("hasAnyRole('PERFORMER', 'ADMIN')")
    @PostMapping
    public void addPerformance(@RequestBody PerformanceDto performanceDto, @RequestParam(required = false) UUID userId, @AuthenticationPrincipal CustomUserDetails user){
        performanceService.addPerformance(performanceDto, userId, user.getId(), isAdmin(user), user.getVenue());
    }

    @PreAuthorize("hasAnyRole('PERFORMER', 'ADMIN')")
    @DeleteMapping
    public void deletePerformance(@RequestParam UUID performanceId){
        performanceService.deletePerformance(performanceId);
    }

    @PreAuthorize("hasAnyRole('PERFORMER', 'ADMIN')")
    @PutMapping("/{performanceId}")
    public void modifyPerformance(@PathVariable UUID performanceId, @RequestParam PerformanceType performanceType, @RequestParam int duration){
        performanceService.modifyPerformance(performanceId, performanceType, duration);
    }

    /**
     * Checks whether the authenticated user has admin privileges.
     *
     * @param auth the Authentication object
     * @return true if the user has ROLE_ADMIN, false otherwise
     */
    private boolean isAdmin(CustomUserDetails user) {
        return user.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
