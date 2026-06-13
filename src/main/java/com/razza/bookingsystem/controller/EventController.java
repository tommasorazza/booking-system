package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.TimeSlot;
import com.razza.bookingsystem.dto.EventByDateResponseDto;
import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.dto.EventResponseDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing events.
 * Provides CRUD operations and event retrieval scoped to the authenticated user's venue.
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    /**
     * Creates a new event within the authenticated user's venue.
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

        return eventService.createEvent(dto, user.getVenue());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{eventId}")
    public EventResponseDto createEventById(@PathVariable UUID eventId, @RequestParam String location, @RequestParam OffsetDateTime date, @AuthenticationPrincipal CustomUserDetails user){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("event", eventId));

        EventRequestDto eventRequestDto = new EventRequestDto();
        eventRequestDto.setName(event.getName());
        eventRequestDto.setDescription(event.getDescription());
        eventRequestDto.setLocation(location);
        eventRequestDto.setDate(date);
        if(event.getBookingPolicy() != null) {
            eventRequestDto.setTotalCapacity(event.getBookingPolicy().getTotalCapacity());
        }
        List<TimeSlot> newSchedule = new ArrayList<>(event.getSchedule().stream().map(slot ->
                new TimeSlot(slot.getUserEmail(), slot.getStartTime(), slot.getEndTime(), slot.getPerformanceId())).collect(Collectors.toList()));
        // this is done in order to create a copy of the old schedule without passing the actual reference, the new
        // list is instanced as well as every slot inside it
        long minutes = ChronoUnit.MINUTES.between(event.getDate(), date);
        for(TimeSlot slot : newSchedule) {
            slot.setStartTime(slot.getStartTime().plusMinutes(minutes));
            slot.setEndTime(slot.getEndTime().plusMinutes(minutes));
        }
        eventRequestDto.setSchedule(newSchedule);
        eventRequestDto.setEighteenPlus(event.getEighteenPlus());
        return eventService.createEvent(eventRequestDto, user.getVenue());
    }

    /**
     * Retrieves a paginated list of events for the authenticated user's venue.
     *
     * @param pageable pagination information (page number, size, sorting)
     * @param user the authenticated user
     * @return a page of EventResponseDto
     */
    @GetMapping
    public Page<EventResponseDto> getAllEvents(Pageable pageable,
                                               @AuthenticationPrincipal CustomUserDetails user) {

        return eventService.getAllEvents(pageable, user.getVenue());
    }

    /**
     * Retrieves a event by its ID within the authenticated user's venue.
     *
     * @param id the ID of the event
     * @param user the authenticated user
     * @return the event as EventResponseDto
     */
    @GetMapping("/{date}")
    public List<EventByDateResponseDto> getEventByDate(@PathVariable LocalDate date,
                                                       @AuthenticationPrincipal CustomUserDetails user) {

        return eventService.getEventsByDate(date, user.getVenue());
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

        return eventService.updateEvent(id, dto, user.getVenue());
    }

    /**
     * Deletes a event by its ID.
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

        eventService.deleteEvent(id, user.getVenue());
    }
}