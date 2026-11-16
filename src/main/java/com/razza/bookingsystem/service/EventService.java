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
 *  Event creation
 *  Event retrieval
 *  Event updates
 *  Event deletion (soft delete via status)
 *  Paginated listing of events
 *
 * Enforces business rules such as venue isolation,
 * capacity constraints and schedule validity.
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
    private final PerformanceService performanceService;

    /** Timezone used to work out the start and end of a day, read from the app configuration. */
    @Value("${app.timezone}")
    private String timezone;

    /**
     * Creates a new event within a venue.
     *
     * Access: ADMIN only.
     *
     * Behavior:
     *  Maps the request DTO to an entity
     *  Validates that the event date is not in the past
     *  Validates the schedule
     *  Creates the booking policy if a total capacity is given, with available capacity equal to total capacity
     *  Assigns the venue and sets the status to CONFIRMED
     *  Sends a schedule reminder email to each performer in the schedule
     *  Persists the event
     *
     * @param dto event data transfer object containing event details
     * @param venue venue the authenticated user belongs to
     * @return the created event as an EventResponseDto
     * @throws PastEventException if event date is in the past
     * @throws EmptyScheduleException if the schedule is empty
     * @throws InvalidScheduleException if the schedule is not valid
     * @throws BadCapacityException if capacity is out of the allowed range
     */
    @Transactional
    public EventResponseDto createEvent(EventRequestDto dto, Venue venue) {

        dto.setLocation(dto.getLocation().toLowerCase().trim());

        Event event = eventMapper.toEntity(dto);

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        isScheduleValid(dto.getSchedule(), dto.getDate(), dto.getLocation(), venue);

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
     * Retrieves the confirmed events happening on a given date within a venue.
     *
     * The start and end of the day are worked out in the configured timezone,
     * and the event dates in the result are converted to that timezone too.
     *
     * @param date the date to look up
     * @param venue venue the authenticated user belongs to
     * @return list of EventByDateResponseDto for the events of that date
     */
    @Transactional
    public List<EventByDateResponseDto> getEventsByDate(LocalDate date, Venue venue) {

        OffsetDateTime start = date.atStartOfDay().atZone(ZoneId.of(timezone)).toOffsetDateTime();
        OffsetDateTime end = date.atStartOfDay().plusDays(1).atZone(ZoneId.of(timezone)).toOffsetDateTime();
        List<Event> events = eventRepository.findByDateAndVenueAndStatus(start, end, venue, Status.CONFIRMED);
        return events.stream().map(event -> new EventByDateResponseDto(event.getName(), event.getDescription(), event.getLocation(), event.getDate().atZoneSameInstant(ZoneId.of(timezone)).toOffsetDateTime(), event.getSchedule())).toList();
    }

    /**
     * Updates an existing event.
     *
     * Access: ADMIN only.
     *
     * Behavior:
     *  Validates that the event exists within the venue
     *  Updates name, description, location and date
     *  Validates the schedule
     *  Handles capacity changes:
     *      Decreasing capacity is only allowed if no active bookings exist
     *      Increasing capacity preserves already booked seats
     *  Checks that the updated date is not in the past
     *  Updates the schedule and the 18 plus flag
     *  Sends email notifications to the users who booked if date or location changes
     *  Sends email notifications to the performers (old and new) if date, location or schedule changes
     *  Persists the updated event
     *
     * @param id event UUID
     * @param dto updated event data
     * @param venue venue the authenticated user belongs to
     * @return updated event as EventResponseDto
     * @throws ResourceNotFoundException if event does not exist within venue
     * @throws EventDecreaseException if attempting to decrease capacity while bookings exist
     * @throws PastEventException if updated event date is in the past
     * @throws BadCapacityException if capacity exceeds allowed maximum
     * @throws EmptyScheduleException if the schedule is empty
     * @throws InvalidScheduleException if the schedule is not valid
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

        dto.setLocation(dto.getLocation().toLowerCase().trim());

        event.setLocation(dto.getLocation());
        event.setDate(dto.getDate());

        isScheduleValid(dto.getSchedule(), dto.getDate(), dto.getLocation(), venue);

        if(dto.getTotalCapacity() != null && event.getBookingPolicy() != null) {
            if (event.getBookingPolicy().getTotalCapacity() > dto.getTotalCapacity()) {
                int activeBookings = bookingRepository.countByEventIdAndStatus(id, Status.CONFIRMED);
                if (activeBookings > 0) {
                    throw new EventDecreaseException(activeBookings);
                }
                event.getBookingPolicy().setTotalCapacity(dto.getTotalCapacity());
                event.getBookingPolicy().setAvailableCapacity(dto.getTotalCapacity());
            } else {
                if(dto.getTotalCapacity() > 10000){
                    throw new BadCapacityException();
                }
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
     * Deletes (soft deletes) an event.
     *
     * Access: ADMIN only.
     *
     * Behavior:
     *  Validates that the event exists within the venue
     *  Prevents deletion if active bookings exist
     *  Prevents deletion of past events
     *  Sends a cancellation email to each performer in the schedule
     *  Marks the event as CANCELLED instead of removing it
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
        /*for (Booking b : bookings) {
            emailService.sendEventDeleteEmail(b.getUser().getEmail(), event.getLocation(), event.getDate());
        } */
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

    /**
     * Validates the schedule of an event.
     *
     * Checks that:
     *  The schedule is not empty
     *  The first slot does not start before the event, nor more than 10 hours after it
     *  The event does not overlap with another confirmed event in the same location
     *  Every slot refers to an existing performance and a registered performer of the venue
     *  Every performer is available on that day and not already busy in another location
     *  Every performance belongs to its performer and is not inactive
     *  Slots do not overlap, end after they start, and last as long as their performance
     *
     * @param schedule the time slots to validate
     * @param eventStartTime the start of the event
     * @param location the location of the event
     * @param venue venue the authenticated user belongs to
     * @throws EmptyScheduleException if the schedule is empty
     * @throws InvalidScheduleException if any of the schedule rules is broken
     * @throws ResourceNotFoundException if a performance does not exist
     * @throws PerformanceNotMatchingUserException if a performance does not belong to the performer of its slot
     * @throws InactivePerformanceException if a performance is inactive
     */
    private void isScheduleValid(List<TimeSlot> schedule, OffsetDateTime eventStartTime, String location, Venue venue) {

        if(schedule.isEmpty()){
            throw new EmptyScheduleException();
        }

        long startMinutes = ChronoUnit.MINUTES.between(eventStartTime, schedule.getFirst().getStartTime());
        if(startMinutes > 600){
            throw InvalidScheduleException.latePerformance();
        }

        if(schedule.getFirst().getStartTime().isBefore(eventStartTime)) {
            throw InvalidScheduleException.slotInThePast();
        }

        if(eventRepository.findByVenue(venue).stream().filter(event -> event.getStatus() == Status.CONFIRMED).anyMatch(event ->
                ((event.getDate().isAfter(eventStartTime) && event.getDate().isBefore(schedule.getLast().getEndTime()) && event.getLocation().equals(location))
                        || (event.getEndTime().isAfter(eventStartTime) && event.getEndTime().isBefore(schedule.getLast().getEndTime()) && event.getLocation().equals(location))
                        || (event.getDate()).isBefore(eventStartTime) && event.getEndTime().isAfter(schedule.getLast().getEndTime()) && event.getLocation().equals(location)
                        || (event.getDate().equals(eventStartTime) && event.getEndTime().equals(schedule.getLast().getEndTime()) && event.getLocation().equals(location))
                ))) {
            throw InvalidScheduleException.overlapping();
        }

        OffsetDateTime endTimeVar = OffsetDateTime.MIN;
        for(TimeSlot slot : schedule) {
            Performance performance = performanceRepository.findById(slot.getPerformanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("performance", slot.getPerformanceId()));
            User user = userRepository.findByEmailAndVenueAndRole(slot.getUserEmail(), venue, Role.PERFORMER)
                    .orElseThrow(() -> InvalidScheduleException.unregisteredEmail(slot.getUserEmail()));
            if(!user.getAvailability().getDay(eventStartTime.getDayOfWeek().name().toLowerCase().trim())) {
                throw InvalidScheduleException.unavailablePerformer(user.getEmail());
            }

            if (performanceService.getPerformances(null,user.getId(),false,user.getVenue()).stream().anyMatch(p -> p.getStartTime().toLocalDate().equals(eventStartTime.toLocalDate()) && !p.getLocation().equals(location))) {
                throw InvalidScheduleException.alreadyBusyPerformer(user.getEmail());
            }

            if(!performance.getUser().getEmail().equals(slot.getUserEmail())){
                throw new PerformanceNotMatchingUserException(slot.getPerformanceId(), slot.getUserEmail());
            }
            if(performance.getPerformanceType() == PerformanceType.INACTIVE){
                throw new InactivePerformanceException(slot.getPerformanceId());
            }
            if(slot.getStartTime().isBefore(endTimeVar)) {
                throw InvalidScheduleException.inconsistentSlots();
            }
            if(slot.getEndTime().isBefore(slot.getStartTime())) {
                throw InvalidScheduleException.slotInThePast();
            }
            endTimeVar = slot.getEndTime();
            long minutes = ChronoUnit.MINUTES.between(slot.getStartTime(), slot.getEndTime());
            if(minutes != performance.getDuration()){
                throw InvalidScheduleException.inconsistentSlots(slot);
            }
        }
    }
}