package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.dto.EventResponseDto;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.PageableOpenAPIConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for managing events.
 * Supports CRUD operations and event listing.
 * Admin-only operations should be secured via Spring Security.
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final PageableOpenAPIConverter pageableOpenAPIConverter;

    /**
     * Creates a new event.
     *
     * @param dto the event details
     * @return the created EventDto
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public EventResponseDto createEvent(@RequestBody EventRequestDto dto, @AuthenticationPrincipal CustomUserDetails user) {
        return eventService.createEvent(
                dto,
                user.getTenant()
        );    }

    /**
     * Returns a list of all events.
     *
     * @return list of EventDto
     */
    @GetMapping
    public Page<EventResponseDto> getAllEvents(Pageable pageable,
                                               @AuthenticationPrincipal CustomUserDetails user) {

        return eventService.getAllEvents(pageable, user.getTenant());
    }

    /**
     * Returns an event by its ID.
     *
     * @param id the event UUID
     * @return the EventDto
     */
    @GetMapping("/{id}")
    public EventResponseDto getEventById(@PathVariable UUID id,
                                         @AuthenticationPrincipal CustomUserDetails user) {

        return eventService.getEventById(id, user.getTenant());
    }
    /**
     * Updates an existing event.
     *
     * @param id the event UUID
     * @param dto the updated event details
     * @return the updated EventDto
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponseDto updateEvent(@PathVariable UUID id,
                                        @RequestBody EventRequestDto dto,
                                        @AuthenticationPrincipal CustomUserDetails user) {

        return eventService.updateEvent(
                id,
                dto,
                user.getTenant()
        );
    }

    /**
     * Deletes an event by its ID.
     *
     * @param id the event UUID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEvent(@PathVariable UUID id,
                            @AuthenticationPrincipal CustomUserDetails user) {

        eventService.deleteEvent(
                id,
                user.getTenant(),
                true
        );
    }

}