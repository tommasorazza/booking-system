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

    @PreAuthorize("hasRole('PERFORMER')")
    @GetMapping
    public List<ScheduledPerformanceDto> getPerformances(@AuthenticationPrincipal CustomUserDetails user){
        return performanceService.getPerformances(user);
    }

    @PreAuthorize("hasRole('PERFORMER')")
    @PostMapping
    public void addPerformance(@RequestBody PerformanceDto performanceDto, @AuthenticationPrincipal CustomUserDetails user){
        String userEmail = user.getEmail();
        Venue venue = user.getVenue();
        performanceService.addPerformance(performanceDto, userEmail, venue);
    }

    @PreAuthorize("hasRole('PERFORMER')")
    @DeleteMapping
    public void deletePerformance(@RequestParam UUID performanceId){
        performanceService.deletePerformance(performanceId);
    }

    @PreAuthorize("hasRole('PERFORMER')")
    @PutMapping("/{performanceId}")
    public void restorePerformance(@PathVariable UUID performanceId, @RequestParam PerformanceType performanceType){
        performanceService.restorePerformance(performanceId, performanceType);
    }

}
