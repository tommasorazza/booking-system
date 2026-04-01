package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.dto.EventResponseDto;
import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.exception.EventDecreaseException;
import com.razza.bookingsystem.exception.EventDeleteException;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.mapper.EventMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
    public EventResponseDto createEvent(EventRequestDto dto, Tenant tenant) {
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
                .orElseThrow(() -> new ResourceNotFoundException("event", id));
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
    public EventResponseDto updateEvent(UUID id, EventRequestDto dto, Tenant tenant) {
        Event event = eventRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("event",id));

        event.setName(dto.getName());
        event.setDescription(dto.getDescription());
        event.setLocation(dto.getLocation());
        event.setDate(dto.getDate());
        if(event.getTotalCapacity() > dto.getTotalCapacity() && bookingRepository.findByEventId(id).isPresent()){
            int activeBookings = bookingRepository.countByEventIdAndStatus(id, Status.CONFIRMED);
            throw new EventDecreaseException(activeBookings);
        } else {
            int bookedSeats = event.getTotalCapacity() - event.getAvailableCapacity();
            event.setTotalCapacity(dto.getTotalCapacity());
            event.setAvailableCapacity(dto.getTotalCapacity() - bookedSeats);
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
            throw new AccessDeniedException("Access denied");
        }
        Event event = eventRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("event", id));
        int activeBookings = bookingRepository
                .countByEventIdAndStatus(id, Status.CONFIRMED);

        if (activeBookings > 0) {
            throw new EventDeleteException(activeBookings);
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