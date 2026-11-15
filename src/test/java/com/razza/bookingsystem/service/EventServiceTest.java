package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.EventByDateResponseDto;
import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.dto.EventResponseDto;
import com.razza.bookingsystem.dto.ScheduledPerformanceDto;
import com.razza.bookingsystem.exception.BadCapacityException;
import com.razza.bookingsystem.exception.EmptyScheduleException;
import com.razza.bookingsystem.exception.EventDecreaseException;
import com.razza.bookingsystem.exception.EventDeleteException;
import com.razza.bookingsystem.exception.InactivePerformanceException;
import com.razza.bookingsystem.exception.InvalidScheduleException;
import com.razza.bookingsystem.exception.PastEventException;
import com.razza.bookingsystem.exception.PerformanceNotMatchingUserException;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.mapper.EventMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.PerformanceRepository;
import com.razza.bookingsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventService.
 *
 * Uses Mockito to isolate the service layer from its dependencies
 * (EventRepository, BookingRepository, EmailService, EventMapper,
 * PerformanceRepository, UserRepository, and PerformanceService).
 * Each test method covers a single behavioral expectation so that
 * failures are easy to trace.
 *
 * Test groups:
 * createEvent      - creation, validation, and initialization rules
 * getEventsByDate  - retrieval and venue/timezone scoping
 * updateEvent      - updates, capacity adjustments, and email notifications
 * deleteEvent      - soft deletion and guard conditions
 * getAllEvents     - pagination and venue scoping
 * isScheduleValid  - schedule validation rules, exercised through createEvent
 *
 * Note on isScheduleValid: this method is private on EventService, so it
 * cannot be tested directly. It is exercised indirectly through
 * createEvent, since createEvent reaches it without any of the extra
 * side effects that updateEvent has (booking counts, capacity branching,
 * email diffing), which keeps each schedule-validation test focused on a
 * single failure condition.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private EmailService emailService;
    @Mock private EventMapper eventMapper;
    @Mock private PerformanceRepository performanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private PerformanceService performanceService;

    @InjectMocks
    private EventService eventService;

    private Venue venue;
    private UUID eventId;
    private OffsetDateTime eventDate;
    private Event event;
    private EventRequestDto requestDto;
    private EventResponseDto responseDto;

    private User performer;
    private Performance performance;
    private TimeSlot validTimeSlot;

    /**
     * Sets up shared test fixtures used across multiple test cases.
     *
     * Creates a venue, a future event with full capacity, a registered
     * and available performer with a matching active performance, and a
     * valid time slot that satisfies every rule enforced by schedule
     * validation. Individual tests may override specific properties to
     * exercise edge cases.
     *
     * Also manually injects the timezone field, since it is normally
     * populated by Spring's Value mechanism, which does not run in a
     * plain Mockito unit test.
     */
    @BeforeEach
    void setUp() {
        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("acme")
                .build();

        eventId = UUID.randomUUID();
        eventDate = OffsetDateTime.now().plusDays(10);

        Availability availability = Availability.builder()
                .monday(true).tuesday(true).wednesday(true).thursday(true)
                .friday(true).saturday(true).sunday(true)
                .build();

        performer = User.builder()
                .id(UUID.randomUUID())
                .name("performer")
                .email("performer@example.com")
                .role(Role.PERFORMER)
                .venue(venue)
                .status(Status.CONFIRMED)
                .availability(availability)
                .build();

        availability.setUser(performer);

        performance = Performance.builder()
                .id(UUID.randomUUID())
                .user(performer)
                .name("Jazz Set")
                .description("A jazz performance")
                .duration(120)
                .performanceType(PerformanceType.LIVE_MUSIC)
                .build();

        validTimeSlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(eventDate.plusMinutes(30))
                .endTime(eventDate.plusMinutes(150))
                .performanceId(performance.getId())
                .build();

        event = Event.builder()
                .id(eventId)
                .name("Concert")
                .description("A great concert")
                .location("amsterdam")
                .date(eventDate)
                .bookingPolicy(new BookingPolicy(200, 200))
                .status(Status.CONFIRMED)
                .venue(venue)
                .schedule(List.of(validTimeSlot))
                .eighteenPlus(false)
                .build();

        requestDto = EventRequestDto.builder()
                .name("Concert")
                .description("A great concert")
                .location("amsterdam")
                .date(eventDate)
                .totalCapacity(200)
                .schedule(List.of(validTimeSlot))
                .eighteenPlus(false)
                .build();

        responseDto = EventResponseDto.builder()
                .name("Concert")
                .description("A great concert")
                .location("amsterdam")
                .date(eventDate)
                .totalCapacity(200)
                .availableCapacity(200)
                .build();

        ReflectionTestUtils.setField(eventService, "timezone", "UTC");
    }

    /**
     * Stubs the repository and service dependencies required for
     * isScheduleValid to succeed using the default performer, performance,
     * and time slot fixtures created in setUp. Call this in any test where
     * execution is expected to pass schedule validation and proceed further
     * into the method under test.
     */
    private void stubValidScheduleDependencies() {
        when(performanceRepository.findById(performance.getId()))
                .thenReturn(Optional.of(performance));
        when(userRepository.findByEmailAndVenueAndRole(performer.getEmail(), venue, Role.PERFORMER))
                .thenReturn(Optional.of(performer));
        when(eventRepository.findByVenue(venue))
                .thenReturn(List.of());
        when(performanceService.getPerformances(null, performer.getId(), false, venue))
                .thenReturn(List.of());
    }

    // ----- createEvent -----

    /**
     * Verifies that a valid request produces a non-null response DTO
     * with the expected name and capacity values.
     */
    @Transactional
    @Test
    void createEvent_success_returnsEventResponseDto() {
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDto(event)).thenReturn(responseDto);
        stubValidScheduleDependencies();

        EventResponseDto result = eventService.createEvent(requestDto, venue);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Concert");
        assertThat(result.getTotalCapacity()).isEqualTo(200);
    }

    /**
     * Verifies that the service assigns the supplied venue to the event
     * before persisting it.
     */
    @Transactional
    @Test
    void createEvent_setsVenueOnEvent() {
        Event eventWithoutVenue = Event.builder()
                .id(eventId)
                .name("Concert")
                .date(eventDate)
                .bookingPolicy(new BookingPolicy(200, 200))
                .schedule(List.of(validTimeSlot))
                // no venue
                .build();

        when(eventMapper.toEntity(requestDto)).thenReturn(eventWithoutVenue);
        when(eventRepository.save(any())).thenReturn(eventWithoutVenue);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        stubValidScheduleDependencies();

        eventService.createEvent(requestDto, venue);

        assertThat(eventWithoutVenue.getVenue()).isEqualTo(venue); // the test passes because eventWithoutVenue object reference is the same throughout the method execution, inside createEvent method the venue is assigned to it and thus the assert passes
    }

    /**
     * Verifies that the service initializes the event status to
     * CONFIRMED regardless of what the mapper returns.
     */
    @Transactional
    @Test
    void createEvent_setsStatusToConfirmed() {
        event.setStatus(null);
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        stubValidScheduleDependencies();

        eventService.createEvent(requestDto, venue);

        assertThat(event.getStatus()).isEqualTo(Status.CONFIRMED);
    }

    /**
     * Verifies that the service sets availableCapacity equal to totalCapacity
     * on creation, even when the mapper did not populate that field.
     */
    @Transactional
    @Test
    void createEvent_setsAvailableCapacityEqualToTotalCapacity() {
        event.getBookingPolicy().setAvailableCapacity(0);
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        stubValidScheduleDependencies();

        eventService.createEvent(requestDto, venue);

        assertThat(event.getBookingPolicy().getAvailableCapacity()).isEqualTo(event.getBookingPolicy().getTotalCapacity());
    }

    /**
     * Verifies that a PastEventException is thrown when the event date
     * (as produced by the mapper) lies in the past, and that the schedule
     * is never validated and the repository save is never called.
     */
    @Transactional
    @Test
    void createEvent_throwsPastEventException_whenDateIsInThePast() {
        Event pastEvent = Event.builder()
                .date(OffsetDateTime.now().minusDays(1))
                .bookingPolicy(new BookingPolicy(100))
                .build();
        when(eventMapper.toEntity(requestDto)).thenReturn(pastEvent);

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(PastEventException.class);

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that a BadCapacityException is thrown when the requested
     * capacity is 10000 or higher, and that no save is attempted. The
     * schedule is valid, so the failure must come from capacity
     * validation, not from schedule validation.
     */
    @Transactional
    @Test
    void createEvent_throwsBadCapacityException_whenCapacityIs10000OrAbove() {
        requestDto.setTotalCapacity(10001);
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        stubValidScheduleDependencies();

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(BadCapacityException.class)
                .hasMessageContaining("10000");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that a BadCapacityException is thrown when the requested
     * capacity is zero or negative.
     */
    @Transactional
    @Test
    void createEvent_throwsBadCapacityException_whenCapacityIsZero() {
        requestDto.setTotalCapacity(0);
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        stubValidScheduleDependencies();

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(BadCapacityException.class);

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that a capacity of 9999 is accepted as a valid upper
     * boundary value
     */
    @Transactional
    @Test
    void createEvent_allowsCapacityAt9999_upperBoundary() {
        requestDto.setTotalCapacity(9999);
        EventResponseDto maxDto = EventResponseDto.builder()
                .totalCapacity(9999)
                .availableCapacity(9999)
                .build();
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(maxDto);
        stubValidScheduleDependencies();

        EventResponseDto result = eventService.createEvent(requestDto, venue);

        assertThat(result.getTotalCapacity()).isEqualTo(9999);
    }

    /**
     * Verifies that an EmptyScheduleException is thrown when the request
     * carries no time slots at all.
     */
    @Transactional
    @Test
    void createEvent_throwsEmptyScheduleException_whenScheduleIsEmpty() {
        requestDto.setSchedule(List.of());
        when(eventMapper.toEntity(requestDto)).thenReturn(event);

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(EmptyScheduleException.class);

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InvalidScheduleException is thrown when the first
     * scheduled slot starts more than ten hours after the event's start
     * time.
     */
    @Transactional
    @Test
    void createEvent_throwsInvalidScheduleException_whenFirstSlotStartsTooLate() {
        TimeSlot lateSlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(eventDate.plusMinutes(700))
                .endTime(eventDate.plusMinutes(820))
                .performanceId(performance.getId())
                .build();
        requestDto.setSchedule(List.of(lateSlot));
        when(eventMapper.toEntity(requestDto)).thenReturn(event);

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining("10 hours");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InvalidScheduleException is thrown when the first
     * scheduled slot starts before the event itself starts.
     */
    @Transactional
    @Test
    void createEvent_throwsInvalidScheduleException_whenFirstSlotStartsBeforeEvent() {
        TimeSlot earlySlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(eventDate.minusMinutes(10))
                .endTime(eventDate.plusMinutes(110))
                .performanceId(performance.getId())
                .build();
        requestDto.setSchedule(List.of(earlySlot));
        when(eventMapper.toEntity(requestDto)).thenReturn(event);

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining("start before the start of the event");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InvalidScheduleException is thrown when the new
     * event's schedule overlaps, at the same location, with an already
     * confirmed event in the venue.
     */
    @Transactional
    @Test
    void createEvent_throwsInvalidScheduleException_whenOverlappingWithAnotherConfirmedEvent() {
        TimeSlot otherSlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(eventDate.plusMinutes(30))
                .endTime(eventDate.plusMinutes(150))
                .performanceId(performance.getId())
                .build();
        Event overlappingEvent = Event.builder()
                .id(UUID.randomUUID())
                .name("Existing show")
                .location("amsterdam")
                .date(eventDate)
                .status(Status.CONFIRMED)
                .schedule(List.of(otherSlot))
                .build();

        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of(overlappingEvent));

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining("overlapping");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that a ResourceNotFoundException is thrown when a scheduled
     * time slot references a performance that does not exist.
     */
    @Transactional
    @Test
    void createEvent_throwsResourceNotFoundException_whenScheduledPerformanceDoesNotExist() {
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of());
        when(performanceRepository.findById(performance.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("performance");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InvalidScheduleException is thrown when a scheduled
     * time slot references an email address that is not registered as a
     * performer within the venue.
     */
    @Transactional
    @Test
    void createEvent_throwsInvalidScheduleException_whenPerformerEmailIsNotRegistered() {
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of());
        when(performanceRepository.findById(performance.getId())).thenReturn(Optional.of(performance));
        when(userRepository.findByEmailAndVenueAndRole(performer.getEmail(), venue, Role.PERFORMER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining("not registered");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InvalidScheduleException is thrown when the
     * scheduled performer is not available, according to their weekly
     * availability, on the day of the event.
     */
    @Transactional
    @Test
    void createEvent_throwsInvalidScheduleException_whenPerformerIsUnavailableOnThatDay() {
        OffsetDateTime unavailableDate = OffsetDateTime.now().plusDays(20)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        Availability mondayOffAvailability = Availability.builder()
                .monday(false).tuesday(true).wednesday(true).thursday(true)
                .friday(true).saturday(true).sunday(true)
                .build();
        User unavailablePerformer = User.builder()
                .id(UUID.randomUUID())
                .name("unavailable performer")
                .email("unavailable@example.com")
                .role(Role.PERFORMER)
                .venue(venue)
                .status(Status.CONFIRMED)
                .availability(mondayOffAvailability)
                .build();
        mondayOffAvailability.setUser(unavailablePerformer);

        Performance mondayPerformance = Performance.builder()
                .id(UUID.randomUUID())
                .user(unavailablePerformer)
                .name("Monday Show")
                .duration(60)
                .performanceType(PerformanceType.LIVE_MUSIC)
                .build();

        TimeSlot mondaySlot = TimeSlot.builder()
                .userEmail(unavailablePerformer.getEmail())
                .startTime(unavailableDate.plusMinutes(30))
                .endTime(unavailableDate.plusMinutes(90))
                .performanceId(mondayPerformance.getId())
                .build();

        requestDto.setDate(unavailableDate);
        requestDto.setSchedule(List.of(mondaySlot));

        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of());
        when(performanceRepository.findById(mondayPerformance.getId())).thenReturn(Optional.of(mondayPerformance));
        when(userRepository.findByEmailAndVenueAndRole(unavailablePerformer.getEmail(), venue, Role.PERFORMER))
                .thenReturn(Optional.of(unavailablePerformer));

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining("not available");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InvalidScheduleException is thrown when the
     * scheduled performer already has another performance, at a
     * different location, on the same date.
     */
    @Transactional
    @Test
    void createEvent_throwsInvalidScheduleException_whenPerformerAlreadyBusyElsewhereOnSameDate() {
        ScheduledPerformanceDto busyElsewhere = new ScheduledPerformanceDto(
                "Other Show", "desc", 60, PerformanceType.LIVE_MUSIC,
                eventDate.plusMinutes(5), "rotterdam");

        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of());
        when(performanceRepository.findById(performance.getId())).thenReturn(Optional.of(performance));
        when(userRepository.findByEmailAndVenueAndRole(performer.getEmail(), venue, Role.PERFORMER))
                .thenReturn(Optional.of(performer));
        when(performanceService.getPerformances(null, performer.getId(), false, venue))
                .thenReturn(List.of(busyElsewhere));

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining("already scheduled");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that a PerformanceNotMatchingUserException is thrown when
     * the referenced performance belongs to a different user than the one
     * registered under the slot's email address.
     */
    @Transactional
    @Test
    void createEvent_throwsPerformanceNotMatchingUserException_whenPerformanceBelongsToDifferentUser() {
        User differentOwner = User.builder()
                .id(UUID.randomUUID())
                .name("someone else")
                .email("someoneelse@example.com")
                .role(Role.PERFORMER)
                .venue(venue)
                .status(Status.CONFIRMED)
                .build();
        Performance mismatchedPerformance = Performance.builder()
                .id(performance.getId())
                .user(differentOwner)
                .name("Jazz Set")
                .duration(120)
                .performanceType(PerformanceType.LIVE_MUSIC)
                .build();

        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of());
        when(performanceRepository.findById(performance.getId())).thenReturn(Optional.of(mismatchedPerformance));
        when(userRepository.findByEmailAndVenueAndRole(performer.getEmail(), venue, Role.PERFORMER))
                .thenReturn(Optional.of(performer));
        when(performanceService.getPerformances(null, performer.getId(), false, venue))
                .thenReturn(List.of());

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(PerformanceNotMatchingUserException.class);

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InactivePerformanceException is thrown when the
     * referenced performance has been deactivated by its owner.
     */
    @Transactional
    @Test
    void createEvent_throwsInactivePerformanceException_whenPerformanceIsInactive() {
        Performance inactivePerformance = Performance.builder()
                .id(performance.getId())
                .user(performer)
                .name("Jazz Set")
                .duration(120)
                .performanceType(PerformanceType.INACTIVE)
                .build();

        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of());
        when(performanceRepository.findById(performance.getId())).thenReturn(Optional.of(inactivePerformance));
        when(userRepository.findByEmailAndVenueAndRole(performer.getEmail(), venue, Role.PERFORMER))
                .thenReturn(Optional.of(performer));
        when(performanceService.getPerformances(null, performer.getId(), false, venue))
                .thenReturn(List.of());

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InactivePerformanceException.class);

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InvalidScheduleException is thrown when two
     * scheduled slots overlap each other in time.
     */
    @Transactional
    @Test
    void createEvent_throwsInvalidScheduleException_whenSlotsOverlapEachOther() {
        TimeSlot firstSlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(eventDate.plusMinutes(30))
                .endTime(eventDate.plusMinutes(150))
                .performanceId(performance.getId())
                .build();
        TimeSlot secondSlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(eventDate.plusMinutes(100)) // starts before firstSlot ends
                .endTime(eventDate.plusMinutes(220))
                .performanceId(performance.getId())
                .build();
        requestDto.setSchedule(List.of(firstSlot, secondSlot));

        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of());
        when(performanceRepository.findById(performance.getId())).thenReturn(Optional.of(performance));
        when(userRepository.findByEmailAndVenueAndRole(performer.getEmail(), venue, Role.PERFORMER))
                .thenReturn(Optional.of(performer));
        when(performanceService.getPerformances(null, performer.getId(), false, venue))
                .thenReturn(List.of());

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining("overlap");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InvalidScheduleException is thrown when a scheduled
     * slot's end time is before its own start time.
     */
    @Transactional
    @Test
    void createEvent_throwsInvalidScheduleException_whenSlotEndsBeforeItStarts() {
        TimeSlot backwardsSlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(eventDate.plusMinutes(60))
                .endTime(eventDate.plusMinutes(30))
                .performanceId(performance.getId())
                .build();
        requestDto.setSchedule(List.of(backwardsSlot));

        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of());
        when(performanceRepository.findById(performance.getId())).thenReturn(Optional.of(performance));
        when(userRepository.findByEmailAndVenueAndRole(performer.getEmail(), venue, Role.PERFORMER))
                .thenReturn(Optional.of(performer));
        when(performanceService.getPerformances(null, performer.getId(), false, venue))
                .thenReturn(List.of());

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining("start before");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an InvalidScheduleException is thrown when a slot's
     * duration does not match the duration of the performance it
     * references.
     */
    @Transactional
    @Test
    void createEvent_throwsInvalidScheduleException_whenSlotDurationDoesNotMatchPerformance() {
        TimeSlot shortSlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(eventDate.plusMinutes(30))
                .endTime(eventDate.plusMinutes(90)) // 60 minutes, performance duration is 120
                .performanceId(performance.getId())
                .build();
        requestDto.setSchedule(List.of(shortSlot));

        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.findByVenue(venue)).thenReturn(List.of());
        when(performanceRepository.findById(performance.getId())).thenReturn(Optional.of(performance));
        when(userRepository.findByEmailAndVenueAndRole(performer.getEmail(), venue, Role.PERFORMER))
                .thenReturn(Optional.of(performer));
        when(performanceService.getPerformances(null, performer.getId(), false, venue))
                .thenReturn(List.of());

        assertThatThrownBy(() -> eventService.createEvent(requestDto, venue))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining("does not have the same duration");

        verify(eventRepository, never()).save(any());
    }


    /**
     * Verifies that getEventsByDate returns a correctly mapped list of
     * EventByDateResponseDto objects when confirmed events exist on the
     * given date, with the event date converted to the configured timezone.
     */
    @Transactional
    @Test
    void getEventsByDate_returnsMappedDtos_whenEventsExist() {
        LocalDate date = LocalDate.now().plusDays(10);

        when(eventRepository.findByDateAndVenueAndStatus(
                any(OffsetDateTime.class), any(OffsetDateTime.class), eq(venue), eq(Status.CONFIRMED)))
                .thenReturn(List.of(event));

        List<EventByDateResponseDto> result = eventService.getEventsByDate(date, venue);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(event.getName());
        assertThat(result.get(0).getDescription()).isEqualTo(event.getDescription());
        assertThat(result.get(0).getLocation()).isEqualTo(event.getLocation());
        assertThat(result.get(0).getDate())
                .isEqualTo(event.getDate().atZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime());
    }

    /**
     * Verifies that an empty list is returned, without errors, when no
     * confirmed events exist on the given date for the venue.
     */
    @Transactional
    @Test
    void getEventsByDate_returnsEmptyList_whenNoEventsExist() {
        LocalDate date = LocalDate.now().plusDays(10);

        when(eventRepository.findByDateAndVenueAndStatus(
                any(OffsetDateTime.class), any(OffsetDateTime.class), eq(venue), eq(Status.CONFIRMED)))
                .thenReturn(List.of());

        List<EventByDateResponseDto> result = eventService.getEventsByDate(date, venue);

        assertThat(result).isEmpty();
    }

    // ----- updateEvent -----

    /**
     * Verifies that a valid update request returns a non-null response DTO.
     */
    @Transactional
    @Test
    void updateEvent_success_returnsUpdatedDto() {
        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDto(event)).thenReturn(responseDto);
        stubValidScheduleDependencies();

        EventResponseDto result = eventService.updateEvent(eventId, requestDto, venue);

        assertThat(result).isNotNull();
    }

    /**
     * Verifies that a ResourceNotFoundException is thrown when the event
     * does not exist for the given venue.
     */
    @Test
    void updateEvent_throwsResourceNotFoundException_whenEventNotFound() {
        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(eventId, requestDto, venue))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    /**
     * Verifies that increasing the total capacity adjusts availableCapacity
     * correctly by preserving the number of already-booked seats.
     *
     * Example: 100 total, 60 available (40 booked) -> 150 total, 110 available.
     */
    @Transactional
    @Test
    void updateEvent_increasingCapacity_updatesAvailableCapacityPreservingBookedSeats() {
        event.getBookingPolicy().setTotalCapacity(100);
        event.getBookingPolicy().setAvailableCapacity(60); // 40 seats are booked

        EventRequestDto increaseDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("amsterdam")
                .date(eventDate)
                .totalCapacity(150)
                .schedule(List.of(validTimeSlot))
                .eighteenPlus(false)
                .build();

        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        stubValidScheduleDependencies();

        eventService.updateEvent(eventId, increaseDto, venue);

        assertThat(event.getBookingPolicy().getTotalCapacity()).isEqualTo(150);
        assertThat(event.getBookingPolicy().getAvailableCapacity()).isEqualTo(110);
    }

    /**
     * Verifies that decreasing the total capacity is permitted when there are
     * no confirmed bookings, and that both capacity fields are updated accordingly.
     */
    @Transactional
    @Test
    void updateEvent_decreasingCapacity_withNoActiveBookings_succeeds() {
        event.getBookingPolicy().setTotalCapacity(200);
        event.getBookingPolicy().setAvailableCapacity(200);

        EventRequestDto decreaseDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("amsterdam")
                .date(eventDate)
                .totalCapacity(100)
                .schedule(List.of(validTimeSlot))
                .eighteenPlus(false)
                .build();

        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(0);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        stubValidScheduleDependencies();

        eventService.updateEvent(eventId, decreaseDto, venue);

        assertThat(event.getBookingPolicy().getTotalCapacity()).isEqualTo(100);
        assertThat(event.getBookingPolicy().getAvailableCapacity()).isEqualTo(100);
    }

    /**
     * Verifies that an EventDecreaseException is thrown, and the
     * repository save is never called, when a capacity decrease is
     * requested while confirmed bookings exist. The exception message
     * must include the number of active bookings.
     */
    @Transactional
    @Test
    void updateEvent_decreasingCapacity_throwsEventDecreaseException_whenActiveBookingsExist() {
        event.getBookingPolicy().setTotalCapacity(200);

        EventRequestDto decreaseDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("amsterdam")
                .date(eventDate)
                .totalCapacity(50)
                .schedule(List.of(validTimeSlot))
                .eighteenPlus(false)
                .build();

        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(5);
        stubValidScheduleDependencies();

        assertThatThrownBy(() -> eventService.updateEvent(eventId, decreaseDto, venue))
                .isInstanceOf(EventDecreaseException.class)
                .hasMessageContaining("5");

        verify(eventRepository, never()).save(any());
    }


    /**
     * Verifies that a PastEventException is thrown, and no save occurs,
     * when the new date supplied in the update request lies in the past.
     * The schedule supplied is valid relative to that past date, so the
     * failure comes from the past-date check rather than from schedule
     * validation, which runs first.
     */
    @Transactional
    @Test
    void updateEvent_throwsPastEventException_whenNewDateIsInThePast() {
        OffsetDateTime pastDate = OffsetDateTime.now().minusDays(1);
        TimeSlot pastSlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(pastDate.plusMinutes(30))
                .endTime(pastDate.plusMinutes(150))
                .performanceId(performance.getId())
                .build();
        EventRequestDto pastDateDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("amsterdam")
                .date(pastDate)
                .totalCapacity(200)
                .schedule(List.of(pastSlot))
                .eighteenPlus(false)
                .build();

        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        stubValidScheduleDependencies();

        assertThatThrownBy(() -> eventService.updateEvent(eventId, pastDateDto, venue))
                .isInstanceOf(PastEventException.class);

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that email notifications are sent to every booked user when
     * the event date is changed. Each call should pass the user's email address,
     * the new date, and the (unchanged) location.
     */
    @Transactional
    @Test
    void updateEvent_sendsEmailNotifications_whenDateChanges() {
        OffsetDateTime newDate = OffsetDateTime.now().plusDays(30);
        String originalLocation = event.getLocation();
        TimeSlot newSlot = TimeSlot.builder()
                .userEmail(performer.getEmail())
                .startTime(newDate.plusMinutes(30))
                .endTime(newDate.plusMinutes(150))
                .performanceId(performance.getId())
                .build();

        EventRequestDto changedDateDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location(originalLocation)
                .date(newDate)
                .totalCapacity(200)
                .schedule(List.of(newSlot))
                .eighteenPlus(false)
                .build();

        User user1 = User.builder().id(UUID.randomUUID()).email("a@example.com").build();
        User user2 = User.builder().id(UUID.randomUUID()).email("b@example.com").build();
        Booking b1 = Booking.builder().user(user1).build();
        Booking b2 = Booking.builder().user(user2).build();

        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        when(bookingRepository.findByEventId(eventId)).thenReturn(List.of(b1, b2));
        stubValidScheduleDependencies();

        eventService.updateEvent(eventId, changedDateDto, venue);

        verify(emailService).sendEventUpdateEmail("a@example.com", newDate, originalLocation);
        verify(emailService).sendEventUpdateEmail("b@example.com", newDate, originalLocation);
    }

    /**
     * Verifies that email notifications are sent to every booked user when
     * the event location is changed. Each call should pass the user's email
     * address, the (unchanged) date, and the new location.
     */
    @Transactional
    @Test
    void updateEvent_sendsEmailNotifications_whenLocationChanges() {
        OffsetDateTime originalDate = event.getDate();

        EventRequestDto changedLocationDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("Rotterdam")
                .date(originalDate)
                .totalCapacity(200)
                .schedule(List.of(validTimeSlot))
                .eighteenPlus(false)
                .build();

        User user1 = User.builder().id(UUID.randomUUID()).email("a@example.com").build();
        Booking b1 = Booking.builder().user(user1).build();

        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        when(bookingRepository.findByEventId(eventId)).thenReturn(List.of(b1));
        stubValidScheduleDependencies();

        eventService.updateEvent(eventId, changedLocationDto, venue);

        verify(emailService).sendEventUpdateEmail("a@example.com", originalDate, "rotterdam");
    }

    /**
     * Verifies that no email notifications are sent, and the booking
     * repository is not queried, when neither the date, the location, nor
     * the schedule changes during an update.
     */
    @Transactional
    @Test
    void updateEvent_doesNotSendEmail_whenNeitherDateNorLocationNorScheduleChanges() {
        EventRequestDto noChangeDto = EventRequestDto.builder()
                .name("New Name").description("New Desc")
                .location(event.getLocation())
                .date(event.getDate())
                .totalCapacity(200)
                .schedule(List.of(validTimeSlot))
                .eighteenPlus(false)
                .build();

        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        stubValidScheduleDependencies();

        eventService.updateEvent(eventId, noChangeDto, venue);

        verify(emailService, never()).sendEventUpdateEmail(any(), any(), any());
        verify(bookingRepository, never()).findByEventId(any());
    }

    // ----- deleteEvent -----

    /**
     * Verifies that deleting a event performs a soft delete by setting its
     * status to CANCELLED and saving the updated entity, rather than
     * removing the record from the database.
     */
    @Transactional
    @Test
    void deleteEvent_success_softDeletesBySettingStatusToCancelled() {
        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(0);

        eventService.deleteEvent(eventId, venue);

        assertThat(event.getStatus()).isEqualTo(Status.CANCELLED);
        verify(eventRepository).save(event);
    }

    /**
     * Verifies that a ResourceNotFoundException is thrown, and no save
     * occurs, when the event to delete is not found for the given venue.
     */
    @Transactional
    @Test
    void deleteEvent_throwsResourceNotFoundException_whenEventNotFound() {
        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.deleteEvent(eventId, venue))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(eventId.toString());

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an EventDeleteException is thrown, and no save
     * occurs, when confirmed bookings exist for the event. The exception
     * message must include the active booking count.
     */
    @Transactional
    @Test
    void deleteEvent_throwsEventDeleteException_whenActiveBookingsExist() {
        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(4);

        assertThatThrownBy(() -> eventService.deleteEvent(eventId, venue))
                .isInstanceOf(EventDeleteException.class)
                .hasMessageContaining("4");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that a PastEventException is thrown, and no save occurs,
     * when an attempt is made to delete a event whose date has already
     * passed.
     */
    @Transactional
    @Test
    void deleteEvent_throwsPastEventException_whenEventIsInThePast() {
        event.setDate(OffsetDateTime.now().minusDays(1));

        when(eventRepository.findByIdAndVenue(eventId, venue)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(0);

        assertThatThrownBy(() -> eventService.deleteEvent(eventId, venue))
                .isInstanceOf(PastEventException.class);

        verify(eventRepository, never()).save(any());
    }

    // ----- getAllEvents -----

    /**
     * Verifies that the service returns a correctly mapped page of
     * EventResponseDto objects when events exist for the venue.
     */
    @Transactional
    @Test
    void getAllEvents_returnsPageOfEventResponseDtos() {
        Pageable pageable = PageRequest.of(0, 10); //first ten elements of page 0
        Page<Event> eventPage = new PageImpl<>(List.of(event)); //creates a fake page containing only the given event
        when(eventRepository.findByVenue(venue, pageable)).thenReturn(eventPage);
        when(eventMapper.toDto(event)).thenReturn(responseDto);

        Page<EventResponseDto> result = eventService.getAllEvents(pageable, venue);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(responseDto);
    }

    /**
     * Verifies that an empty page is returned without errors when no events
     * exist for the venue.
     */
    @Transactional
    @Test
    void getAllEvents_returnsEmptyPage_whenNoEventsExist() {
        Pageable pageable = PageRequest.of(0, 10);
        when(eventRepository.findByVenue(venue, pageable)).thenReturn(Page.empty());

        Page<EventResponseDto> result = eventService.getAllEvents(pageable, venue);

        assertThat(result).isEmpty();
    }
}