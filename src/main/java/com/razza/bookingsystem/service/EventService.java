package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.EventByDateResponseDto;
import com.razza.bookingsystem.dto.EventResponseDto;
import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.exception.*;
import com.razza.bookingsystem.mapper.EventMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.PerformanceRepository;
import com.razza.bookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

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
 * Enforces business rules such as venue isolation
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
    private final UserRepository userRepository;
    private final PerformanceRepository performanceRepository;
    private final EmailService emailService;
    private final EventMapper eventMapper;

    @Value("${app.timezone}")
    private String timezone;

    /**
     * Creates a new event within a venue.
     *
     * Access: ADMIN only.
     *
     * Behavior:
     * - maps the request DTO to an entity
     * - assigns the venue
     * - initializes version and status
     * - sets available capacity equal to total capacity
     * - validates that event date is not in the past
     * - persists the event
     *
     * @param dto event data transfer object containing event details
     * @param venue venue the authenticated user belongs to
     * @return the created event as an EventResponseDto
     * @throws PastEventException if event date is in the past
     * @throws IllegalArgumentException if capacity exceeds allowed maximum
     */
    @Transactional
    public EventResponseDto createEvent(EventRequestDto dto, Venue venue) {

        Event event = eventMapper.toEntity(dto);

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        isScheduleValid(dto.getSchedule(), dto.getDate(), venue);

        if(dto.getTotalCapacity() != null){
            BookingPolicy bookingPolicy = new BookingPolicy(dto.getTotalCapacity());
            event.setBookingPolicy(bookingPolicy);
        } else {
            event.setBookingPolicy(null);
        }

        event.setVenue(venue);
        event.setStatus(Status.CONFIRMED);
        event.getSchedule().stream().map(TimeSlot::getUserEmail)
                    .forEach
                (email -> emailService.sendScheduleReminder(email, event.getLocation(), event.getDate()));
        Event saved = eventRepository.save(event);
        return eventMapper.toDto(saved);
    }

    /**
     * Retrieves a event by its unique identifier within a venue.
     *
     * @param id event UUID
     * @param venue venue the authenticated user belongs to
     * @return corresponding EventResponseDto
     * @throws ResourceNotFoundException if event does not exist within venue
     */
    @Transactional
    public List<EventByDateResponseDto> getEventsByDate(LocalDate date, Venue venue) {

        OffsetDateTime start = date.atStartOfDay().atZone(ZoneId.of(timezone)).toOffsetDateTime();
        OffsetDateTime end = date.atStartOfDay().plusDays(1).atZone(ZoneId.of(timezone)).toOffsetDateTime();
        List<Event> events = eventRepository.findByDateAndVenue(start, end, venue);
        return events.stream().map(event -> new EventByDateResponseDto(event.getName(), event.getDescription(), event.getLocation(), event.getDate().atZoneSameInstant(ZoneId.of(timezone)).toOffsetDateTime(), event.getSchedule())).toList();
    }

    /**
     * Updates an existing event.
     *
     * Access: ADMIN only.
     *
     * Behavior:
     * - validates that the event exists within the venue
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
     * @param venue venue the authenticated user belongs to
     * @return updated event as EventResponseDto
     * @throws ResourceNotFoundException if event does not exist within venue
     * @throws EventDecreaseException if attempting to decrease capacity while bookings exist
     * @throws PastEventException if updated event date is in the past
     * @throws IllegalArgumentException if capacity exceeds allowed maximum
     */
    @Transactional
    public EventResponseDto updateEvent(UUID id, EventRequestDto dto, Venue venue) {
        Event event = eventRepository.findByIdAndVenue(id, venue)
                .orElseThrow(() -> new ResourceNotFoundException("event", id));

        event.setName(dto.getName());
        event.setDescription(dto.getDescription());

        OffsetDateTime date = event.getDate();
        String location = event.getLocation();
        List<TimeSlot> schedule = event.getSchedule();

        event.setLocation(dto.getLocation());
        event.setDate(dto.getDate());

        isScheduleValid(dto.getSchedule(), dto.getDate(), venue);

        if(dto.getTotalCapacity() != null && event.getBookingPolicy() != null) {
            if (event.getBookingPolicy().getTotalCapacity() > dto.getTotalCapacity()) {
                int activeBookings = bookingRepository.countByEventIdAndStatus(id, Status.CONFIRMED);
                if (activeBookings > 0) {
                    throw new EventDecreaseException(activeBookings);
                }
                event.getBookingPolicy().setTotalCapacity(dto.getTotalCapacity());
                event.getBookingPolicy().setAvailableCapacity(dto.getTotalCapacity());
            } else {
                int bookedSeats = event.getBookingPolicy().getTotalCapacity() - event.getBookingPolicy().getAvailableCapacity();
                event.getBookingPolicy().setTotalCapacity(dto.getTotalCapacity());
                event.getBookingPolicy().setAvailableCapacity(dto.getTotalCapacity() - bookedSeats);
            }
        } else if(dto.getTotalCapacity() == null){
            event.setBookingPolicy(null);
        } else {
            event.setBookingPolicy(new BookingPolicy(dto.getTotalCapacity()));
        }

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        event.setSchedule(dto.getSchedule());
        event.setEighteenPlus(dto.getEighteenPlus());

        Event updated = eventRepository.save(event);

        if(!date.equals(dto.getDate()) || !location.equals(dto.getLocation())) {
            Collection<Booking> bookings = bookingRepository.findByEventId(id);
            for (Booking b : bookings) {
                emailService.sendEventUpdateEmail(b.getUser().getEmail(), dto.getDate(), dto.getLocation());
            }
        }

        if(!date.equals(dto.getDate()) || !location.equals(dto.getLocation()) || !schedule.equals(dto.getSchedule())) {
            List<String> oldEmails = schedule.stream().map(TimeSlot::getUserEmail).toList();
            List<String> newEmails = dto.getSchedule().stream().map(TimeSlot::getUserEmail).toList();
            Stream.concat(oldEmails.stream(), newEmails.stream()).distinct().forEach(e-> emailService.sendEventUpdateEmail(e,dto.getDate(),dto.getLocation()));
        }

        return eventMapper.toDto(updated);
    }

    /**
     * Deletes (soft deletes) a event.
     *
     * Access: ADMIN only.
     *
     * Behavior:
     * - validates that the event exists within the venue
     * - prevents deletion if active bookings exist
     * - prevents deletion of past events
     * - marks event as CANCELLED instead of removing it
     *
     * @param id event UUID
     * @param venue venue the authenticated user belongs to
     * @throws ResourceNotFoundException if event does not exist within venue
     * @throws EventDeleteException if active bookings exist for the event
     * @throws PastEventException if event is already in the past
     */
    @Transactional
    public void deleteEvent(UUID id, Venue venue) {
        Event event = eventRepository.findByIdAndVenue(id, venue)
                .orElseThrow(() -> new ResourceNotFoundException("event", id));

        int activeBookings = bookingRepository
                .countByEventIdAndStatus(id, Status.CONFIRMED);

        if (activeBookings > 0) {
            throw new EventDeleteException(activeBookings);
        }

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }
        Collection<Booking> bookings = bookingRepository.findByEventId(id);
        for (Booking b : bookings) {
            emailService.sendEventDeleteEmail(b.getUser().getEmail(), event.getLocation(), event.getDate());
        }
        for(String email : event.getSchedule().stream().map(TimeSlot::getUserEmail).toList()) {
           emailService.sendEventDeleteEmail(email, event.getLocation(), event.getDate());
        }
        event.setStatus(Status.CANCELLED);
        eventRepository.save(event);
    }

    /**
     * Retrieves a paginated list of events within a venue.
     *
     * @param pageable pagination and sorting configuration
     * @param venue venue the authenticated user belongs to
     * @return page of EventResponseDto
     */
    public Page<EventResponseDto> getAllEvents(Pageable pageable, Venue venue) {
        return eventRepository.findByVenue(venue, pageable)
                .map(eventMapper::toDto);
    }

    private void isScheduleValid(List<TimeSlot> schedule, OffsetDateTime eventDate, Venue venue) {

        if(schedule.isEmpty()){
            throw new EmptyScheduleException();
        }

        long startMinutes = ChronoUnit.MINUTES.between(eventDate, schedule.get(0).getStartTime());
        if(startMinutes > 600){
            throw new InvalidScheduleException(startMinutes);
        }

        for(TimeSlot slot : schedule) {
            Performance performance = performanceRepository.findById(slot.getPerformanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("performance", slot.getPerformanceId()));
            if (userRepository.findByEmailAndVenueAndRole(slot.getUserEmail(), venue, Role.PERFORMER).isEmpty()){
                throw new InvalidScheduleException(slot.getUserEmail());
            }
            if(!performance.getUser().getEmail().equals(slot.getUserEmail())){
                throw new PerformanceNotMatchingUserException(slot.getPerformanceId(), slot.getUserEmail());
            }
            if(performance.getPerformanceType() == PerformanceType.INACTIVE){
                throw new InactivePerformanceException(slot.getPerformanceId());
            }

            if(slot.getEndTime().isBefore(slot.getStartTime())) {
                throw new InvalidScheduleException();
            }
            long minutes = ChronoUnit.MINUTES.between(slot.getStartTime(), slot.getEndTime());
            if(minutes != performance.getDuration()){
                throw new InvalidScheduleException(slot);
            }
        }
    }
}