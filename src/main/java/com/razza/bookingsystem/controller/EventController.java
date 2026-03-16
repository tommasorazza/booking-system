package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.dto.EventDto;
import com.razza.bookingsystem.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.PageableOpenAPIConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    @PostMapping
    public EventDto createEvent(@RequestBody EventDto dto) {
        return eventService.createEvent(dto);
    }

    /**
     * Returns a list of all events.
     *
     * @return list of EventDto
     */
    @GetMapping
    public Page<EventDto> getAllEvents(Pageable pageable) {
        return eventService.getAllEvents(pageable);
    }

    /**
     * Returns an event by its ID.
     *
     * @param id the event UUID
     * @return the EventDto
     */
    @GetMapping("/{id}")
    public EventDto getEventById(@PathVariable UUID id) {
        return eventService.getEventById(id);
    }

    /**
     * Updates an existing event.
     *
     * @param id the event UUID
     * @param dto the updated event details
     * @return the updated EventDto
     */
    @PutMapping("/{id}")
    public EventDto updateEvent(@PathVariable UUID id,
                                @RequestBody EventDto dto) {
        return eventService.updateEvent(id, dto);
    }

    /**
     * Deletes an event by its ID.
     *
     * @param id the event UUID
     */
    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
    }
}