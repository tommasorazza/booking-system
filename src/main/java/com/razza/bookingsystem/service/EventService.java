package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.dto.EventDto;
import com.razza.bookingsystem.mapper.EventMapper;
import com.razza.bookingsystem.repository.BookingRepository;
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
    private final BookingRepository bookingRepository;
    private final EventMapper eventMapper;


    /**
     * Creates a new event in the system.
     *
     * @param dto the event data transfer object containing event details
     * @return the persisted event as a DTO
     */
    public EventDto createEvent(EventDto dto, Tenant tenant, Boolean isAdmin) {
        if (!isAdmin) {
            throw new RuntimeException("Only admins can create events");
        }
        Event event = eventMapper.toEntity(dto);
        event.setTenant(tenant);
        event.setVersion(0L);
        event.setStatus(Status.CONFIRMED);
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
    public EventDto getEventById(UUID id, Tenant tenant) {
        Event event = eventRepository.findByIdAndTenant(id,tenant)
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
    public EventDto updateEvent(UUID id, EventDto dto, Tenant tenant, Boolean isAdmin) {
        if (!isAdmin) {
            throw new RuntimeException("Only admins can create events");
        }
        Event event = eventRepository.findByIdAndTenant(id, tenant)
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
    public void deleteEvent(UUID id, Tenant tenant,  Boolean isAdmin) {
        if (!isAdmin) {
            throw new RuntimeException("Only admins can create events");
        }
        Event event = eventRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        boolean hasActiveBookings = bookingRepository
                .existsByEventIdAndStatus(id, Status.CONFIRMED);

        if (hasActiveBookings) {
            throw new RuntimeException("Cannot delete event with active bookings");
        }

        event.setStatus(com.razza.bookingsystem.domain.Status.CANCELLED);
        eventRepository.save(event);
    }
    /**
     * Retrieves a paginaPage<Event> findByTenantId(UUID tenantId, Pageable pageable);ted list of events.
     *
     * @param pageable pagination and sorting configuration
     * @return a page of EventDto objects
     */
    public Page<EventDto> getAllEvents(Pageable pageable, Tenant tenant) {
        return eventRepository.findByTenant(tenant, pageable)
                .map(eventMapper::toDto);
    }

}