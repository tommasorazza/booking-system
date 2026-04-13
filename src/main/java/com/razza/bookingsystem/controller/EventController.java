package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.dto.EventResponseDto;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for managing events.
 * Provides CRUD operations and event retrieval scoped to the authenticated user's tenant.
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    /**
     * Creates a new event within the authenticated user's tenant.
     *
     * Access restricted to users with ADMIN role.
     *
     * @param dto request containing event details
     * @param user the authenticated user
     * @return the created event as EventResponseDto
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public EventResponseDto createEvent(@RequestBody EventRequestDto dto,
                                        @AuthenticationPrincipal CustomUserDetails user) {

        return eventService.createEvent(dto, user.getTenant());
    }

    /**
     * Retrieves a paginated list of events for the authenticated user's tenant.
     *
     * @param pageable pagination information (page number, size, sorting)
     * @param user the authenticated user
     * @return a page of EventResponseDto
     */
    @GetMapping
    public Page<EventResponseDto> getAllEvents(Pageable pageable,
                                               @AuthenticationPrincipal CustomUserDetails user) {

        return eventService.getAllEvents(pageable, user.getTenant());
    }

    /**
     * Retrieves an event by its ID within the authenticated user's tenant.
     *
     * @param id the ID of the event
     * @param user the authenticated user
     * @return the event as EventResponseDto
     */
    @GetMapping("/{id}")
    public EventResponseDto getEventById(@PathVariable UUID id,
                                         @AuthenticationPrincipal CustomUserDetails user) {

        return eventService.getEventById(id, user.getTenant());
    }

    /**
     * Updates an existing event.
     *
     * Access restricted to users with ADMIN role.
     *
     * @param id the ID of the event to update
     * @param dto request containing updated event details
     * @param user the authenticated user
     * @return the updated event as EventResponseDto
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponseDto updateEvent(@PathVariable UUID id,
                                        @RequestBody EventRequestDto dto,
                                        @AuthenticationPrincipal CustomUserDetails user) {

        return eventService.updateEvent(id, dto, user.getTenant());
    }

    /**
     * Deletes an event by its ID.
     *
     * Access restricted to users with ADMIN role.
     *
     * @param id the ID of the event to delete
     * @param user the authenticated user
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEvent(@PathVariable UUID id,
                            @AuthenticationPrincipal CustomUserDetails user) {

        eventService.deleteEvent(id, user.getTenant());
    }
}