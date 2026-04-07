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
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for managing Event entities.
 *
 * Handles:
 * - event creation
 * - event retrieval
 * - event updates
 * - event deletion (soft delete via status)
 * - paginated listing of events
 *
 * Enforces business rules such as tenant isolation
 * and capacity constraints.
 *
 * Access: create,update and delete methods are intended to be accessed by ADMIN users only.
 * Access control is enforced at the controller layer using @PreAuthorize annotations.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final EventMapper eventMapper;

    /**
     * Creates a new event within a tenant.
     *
     * Access: ADMIN only.
     *
     * Behavior:
     * - maps the request DTO to an entity
     * - assigns the tenant
     * - initializes version and status
     * - sets available capacity equal to total capacity
     * - persists the event
     *
     * @param dto the event data transfer object containing event details
     * @param tenant the tenant the authenticated user belongs to
     * @return the created event as an EventResponseDto
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
     * Retrieves an event by its unique identifier within a tenant.
     *
     * @param id the UUID of the event
     * @param tenant the tenant the authenticated user belongs to
     * @return the corresponding EventResponseDto
     * @throws ResourceNotFoundException if the event does not exist within the tenant
     */
    public EventResponseDto getEventById(UUID id, Tenant tenant) {
        Event event = eventRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("event", id));
        return eventMapper.toDto(event);
    }

    /**
     * Updates an existing event.
     *
     * Access: ADMIN only.
     *
     * Behavior:
     * - validates that the event exists within the tenant
     * - updates basic event fields
     * - handles capacity changes:
     *   - decreasing capacity is only allowed if there are no active bookings
     *   - increasing capacity preserves already booked seats
     * - persists the updated event
     *
     * @param id the UUID of the event to update
     * @param dto the DTO containing updated event information
     * @param tenant the tenant the authenticated user belongs to
     * @return the updated event as an EventResponseDto
     * @throws ResourceNotFoundException if the event does not exist within the tenant
     * @throws EventDecreaseException if attempting to decrease capacity while bookings exist
     */
    @Transactional
    public EventResponseDto updateEvent(UUID id, EventRequestDto dto, Tenant tenant) {
        Event event = eventRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("event", id));

        event.setName(dto.getName());
        event.setDescription(dto.getDescription());
        event.setLocation(dto.getLocation());
        event.setDate(dto.getDate());

        if (event.getTotalCapacity() > dto.getTotalCapacity()) {
            int activeBookings = bookingRepository.countByEventIdAndStatus(id, Status.CONFIRMED);
            if (activeBookings > 0) {
                throw new EventDecreaseException(activeBookings);
            }
            event.setTotalCapacity(dto.getTotalCapacity());
            event.setAvailableCapacity(dto.getTotalCapacity());
        } else {
            int bookedSeats = event.getTotalCapacity() - event.getAvailableCapacity();
            event.setTotalCapacity(dto.getTotalCapacity());
            event.setAvailableCapacity(dto.getTotalCapacity() - bookedSeats);
        }

        Event updated = eventRepository.save(event);
        return eventMapper.toDto(updated);
    }

    /**
     * Deletes (soft deletes) an event.
     *
     * Access: ADMIN only.
     *
     * Behavior:
     * - validates that the event exists within the tenant
     * - prevents deletion if active bookings exist
     * - marks the event as CANCELLED instead of removing it
     *
     * @param id the UUID of the event to delete
     * @param tenant the tenant the authenticated user belongs to
     * @throws ResourceNotFoundException if the event does not exist within the tenant
     * @throws EventDeleteException if active bookings exist for the event
     */
    @Transactional
    public void deleteEvent(UUID id, Tenant tenant) {
        Event event = eventRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("event", id));

        int activeBookings = bookingRepository
                .countByEventIdAndStatus(id, Status.CONFIRMED);

        if (activeBookings > 0) {
            throw new EventDeleteException(activeBookings);
        }

        event.setStatus(Status.CANCELLED);
        eventRepository.save(event);
    }

    /**
     * Retrieves a paginated list of events within a tenant.
     *
     * @param pageable pagination and sorting configuration
     * @param tenant the tenant the authenticated user belongs to
     * @return a Page of EventResponseDto
     */
    public Page<EventResponseDto> getAllEvents(Pageable pageable, Tenant tenant) {
        return eventRepository.findByTenant(tenant, pageable)
                .map(eventMapper::toDto);
    }
}