package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Booking;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.dto.EventResponseDto;
import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.exception.EventDecreaseException;
import com.razza.bookingsystem.exception.EventDeleteException;
import com.razza.bookingsystem.exception.PastEventException;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.mapper.EventMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
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
 * Access: create, update and delete methods are intended to be accessed by ADMIN users only.
 * Access control is enforced at the controller layer using @PreAuthorize annotations.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;
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
     * - validates that event date is not in the past
     * - persists the event
     *
     * @param dto event data transfer object containing event details
     * @param tenant tenant the authenticated user belongs to
     * @return the created event as an EventResponseDto
     * @throws PastEventException if event date is in the past
     * @throws IllegalArgumentException if capacity exceeds allowed maximum
     */
    @Transactional
    public EventResponseDto createEvent(EventRequestDto dto, Tenant tenant) {
        Event event = eventMapper.toEntity(dto);
        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }
        event.setTenant(tenant);
        event.setStatus(Status.CONFIRMED);
        event.setAvailableCapacity(event.getTotalCapacity());
        if(event.getTotalCapacity() > 10000){
            throw new IllegalArgumentException("capacity cannot exceed 10000");
        }
        Event saved = eventRepository.save(event);
        return eventMapper.toDto(saved);
    }

    /**
     * Retrieves an event by its unique identifier within a tenant.
     *
     * @param id event UUID
     * @param tenant tenant the authenticated user belongs to
     * @return corresponding EventResponseDto
     * @throws ResourceNotFoundException if event does not exist within tenant
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
     * - updates event fields
     * - handles capacity changes:
     *   - decreasing capacity is only allowed if no active bookings exist
     *   - increasing capacity preserves already booked seats
     * - sends email notifications if date or location changes
     * - validates constraints after update
     * - persists the updated event
     *
     * @param id event UUID
     * @param dto updated event data
     * @param tenant tenant the authenticated user belongs to
     * @return updated event as EventResponseDto
     * @throws ResourceNotFoundException if event does not exist within tenant
     * @throws EventDecreaseException if attempting to decrease capacity while bookings exist
     * @throws PastEventException if updated event date is in the past
     * @throws IllegalArgumentException if capacity exceeds allowed maximum
     */
    @Transactional
    public EventResponseDto updateEvent(UUID id, EventRequestDto dto, Tenant tenant) {
        Event event = eventRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("event", id));

        event.setName(dto.getName());
        event.setDescription(dto.getDescription());

        OffsetDateTime date = event.getDate();
        String location = event.getLocation();

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

        if(event.getTotalCapacity() > 10000){
            throw new IllegalArgumentException("capacity cannot exceed 10000");
        }

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        Event updated = eventRepository.save(event);

        if(!date.equals(dto.getDate()) || !location.equals(dto.getLocation())) {
            Collection<Booking> bookings = bookingRepository.findByEventId(id);
            for (Booking b : bookings) {
                emailService.sendEventUpdateEmail(b.getUser().getEmail(), dto.getDate(), dto.getLocation());
            }
        }

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
     * - prevents deletion of past events
     * - marks event as CANCELLED instead of removing it
     *
     * @param id event UUID
     * @param tenant tenant the authenticated user belongs to
     * @throws ResourceNotFoundException if event does not exist within tenant
     * @throws EventDeleteException if active bookings exist for the event
     * @throws PastEventException if event is already in the past
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

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        event.setStatus(Status.CANCELLED);
        eventRepository.save(event);
    }

    /**
     * Retrieves a paginated list of events within a tenant.
     *
     * @param pageable pagination and sorting configuration
     * @param tenant tenant the authenticated user belongs to
     * @return page of EventResponseDto
     */
    public Page<EventResponseDto> getAllEvents(Pageable pageable, Tenant tenant) {
        return eventRepository.findByTenant(tenant, pageable)
                .map(eventMapper::toDto);
    }
}