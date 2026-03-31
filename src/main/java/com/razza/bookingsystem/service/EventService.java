package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.dto.EventResponseDto;
import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.mapper.EventMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import jakarta.transaction.Transactional;
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
    @Transactional
    public EventResponseDto createEvent(EventRequestDto dto, Tenant tenant, Boolean isAdmin) {
        if (!isAdmin) {
            throw new RuntimeException("Only admins can create events");
        }
        Event event = eventMapper.toEntity(dto);
        event.setTenant(tenant);
        event.setVersion(0L);
        event.setStatus(Status.CONFIRMED);
        event.setAvailableCapacity(event.getTotalCapacity());
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
    public EventResponseDto getEventById(UUID id, Tenant tenant) {
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
    @Transactional
    public EventResponseDto updateEvent(UUID id, EventRequestDto dto, Tenant tenant, Boolean isAdmin) {
        if (!isAdmin) {
            throw new RuntimeException("Only admins can create events");
        }
        Event event = eventRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setName(dto.getName());
        event.setDescription(dto.getDescription());
        event.setLocation(dto.getLocation());
        event.setDate(dto.getDate());
        if(event.getTotalCapacity() > dto.getTotalCapacity() && bookingRepository.findByEventId(id).isPresent()){
            throw new RuntimeException("Can't decrease capacity since bookings already present");
        } else {
            int temp = event.getTotalCapacity() - event.getAvailableCapacity();
            event.setTotalCapacity(dto.getTotalCapacity());
            event.setAvailableCapacity(dto.getTotalCapacity());
            event.setAvailableCapacity(dto.getTotalCapacity() - temp);
        }
        Event updated = eventRepository.save(event);
        return eventMapper.toDto(updated);
    }

    /**
     * Deletes an event from the system.
     *
     * @param id the UUID of the event to delete
     */
    @Transactional
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
    public Page<EventResponseDto> getAllEvents(Pageable pageable, Tenant tenant) {
        return eventRepository.findByTenant(tenant, pageable)
                .map(eventMapper::toDto);
    }

}