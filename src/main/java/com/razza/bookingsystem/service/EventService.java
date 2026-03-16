package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.dto.EventDto;
import com.razza.bookingsystem.mapper.EventMapper;
import com.razza.bookingsystem.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for managing Event entities.
 * Handles business logic related to event creation, retrieval,
 * updates, deletion, and paginated listing.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    /**
     * Creates a new event in the system.
     *
     * @param dto the event data transfer object containing event details
     * @return the persisted event as a DTO
     */
    public EventDto createEvent(EventDto dto) {
        Event event = eventMapper.toEntity(dto);
        Event saved = eventRepository.save(event);
        return eventMapper.toDto(saved);
    }

    /**
     * Retrieves an event by its unique identifier.
     *
     * @param id the UUID of the event
     * @return the corresponding EventDto
     * @throws RuntimeException if the event cannot be found
     */
    public EventDto getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return eventMapper.toDto(event);
    }

    /**
     * Updates an existing event with new data.
     *
     * @param id the UUID of the event to update
     * @param dto the DTO containing updated event information
     * @return the updated event as a DTO
     * @throws RuntimeException if the event cannot be found
     */
    public EventDto updateEvent(UUID id, EventDto dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setName(dto.getName());
        event.setDescription(dto.getDescription());
        event.setLocation(dto.getLocation());
        event.setDate(dto.getDate());

        Event updated = eventRepository.save(event);
        return eventMapper.toDto(updated);
    }

    /**
     * Deletes an event from the system.
     *
     * @param id the UUID of the event to delete
     */
    public void deleteEvent(UUID id) {
        eventRepository.deleteById(id);
    }

    /**
     * Retrieves a paginated list of events.
     *
     * @param pageable pagination and sorting configuration
     * @return a page of EventDto objects
     */
    public Page<EventDto> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable)
                .map(eventMapper::toDto);
    }

}