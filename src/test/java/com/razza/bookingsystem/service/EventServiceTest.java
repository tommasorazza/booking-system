package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Booking;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.dto.EventResponseDto;
import com.razza.bookingsystem.exception.EventDecreaseException;
import com.razza.bookingsystem.exception.EventDeleteException;
import com.razza.bookingsystem.exception.PastEventException;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.mapper.EventMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventService}.
 *
 * Uses Mockito to isolate the service layer from its dependencies
 * ({@link EventRepository}, {@link BookingRepository}, {@link EmailService},
 * and {@link EventMapper}). Each test method covers a single behavioral
 * expectation so that failures are easy to trace.
 *
 * Test groups:
 * createEvent   - creation, validation, and initialization rules
 * getEventById  - retrieval and tenant scoping
 * updateEvent   - updates, capacity adjustments, and email notifications
 * deleteEvent   - soft deletion and guard conditions
 * getAllEvents  - pagination and tenant scoping
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private EmailService emailService;
    @Mock private EventMapper eventMapper;

    @InjectMocks
    private EventService eventService;

    private Tenant tenant;
    private UUID eventId;
    private Event event;
    private EventRequestDto requestDto;
    private EventResponseDto responseDto;

    /**
     * Sets up shared test fixtures used across multiple test cases.
     * Creates a tenant, a future event with full capacity, and the
     * corresponding request/response DTOs.
     */
    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("acme")
                .build();

        eventId = UUID.randomUUID();

        event = Event.builder()
                .id(eventId)
                .name("Concert")
                .description("A great concert")
                .location("Amsterdam")
                .date(OffsetDateTime.now().plusDays(10))
                .totalCapacity(200)
                .availableCapacity(200)
                .status(Status.CONFIRMED)
                .tenant(tenant)
                .build();

        requestDto = EventRequestDto.builder()
                .name("Concert")
                .description("A great concert")
                .location("Amsterdam")
                .date(OffsetDateTime.now().plusDays(10))
                .totalCapacity(200)
                .build();

        responseDto = EventResponseDto.builder()
                .id(eventId)
                .name("Concert")
                .description("A great concert")
                .location("Amsterdam")
                .date(requestDto.getDate())
                .totalCapacity(200)
                .availableCapacity(200)
                .build();
    }

    /**
     * Verifies that a valid request produces a non-null response DTO
     * with the expected name and capacity values.
     */
    @Test
    void createEvent_success_returnsEventResponseDto() {
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDto(event)).thenReturn(responseDto);

        EventResponseDto result = eventService.createEvent(requestDto, tenant);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Concert");
        assertThat(result.getTotalCapacity()).isEqualTo(200);
    }

    /**
     * Verifies that the service assigns the supplied tenant to the event
     * before persisting it.
     */
    @Test
    void createEvent_setsTenantOnEvent() {
        Event eventWithoutTenant = Event.builder()
                .id(eventId)
                .name("Concert")
                .date(OffsetDateTime.now().plusDays(10))
                .totalCapacity(200)
                .availableCapacity(200)
                // no tenant
                .build();

        when(eventMapper.toEntity(requestDto)).thenReturn(eventWithoutTenant);
        when(eventRepository.save(any())).thenReturn(eventWithoutTenant);
        when(eventMapper.toDto(any())).thenReturn(responseDto);

        eventService.createEvent(requestDto, tenant);

        assertThat(eventWithoutTenant.getTenant()).isEqualTo(tenant); // the test passes because eventWithoutTenant object reference is the same throughout the method execution, inside createEvent method the tenant is assigned to it and thus the assert passes
    }

    /**
     * Verifies that the service initializes the event status to
     * {@link Status#CONFIRMED} regardless of what the mapper returns.
     */
    @Test
    void createEvent_setsStatusToConfirmed() {
        event.setStatus(null);
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);

        eventService.createEvent(requestDto, tenant);

        assertThat(event.getStatus()).isEqualTo(Status.CONFIRMED);
    }

    /**
     * Verifies that the service sets availableCapacity equal to totalCapacity
     * on creation, even when the mapper did not populate that field.
     */
    @Test
    void createEvent_setsAvailableCapacityEqualToTotalCapacity() {
        event.setAvailableCapacity(0);
        when(eventMapper.toEntity(requestDto)).thenReturn(event);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);

        eventService.createEvent(requestDto, tenant);

        assertThat(event.getAvailableCapacity()).isEqualTo(event.getTotalCapacity());
    }

    /**
     * Verifies that a {@link PastEventException} is thrown when the event
     * date lies in the past, and that the repository save is never called.
     */
    @Test
    void createEvent_throwsPastEventException_whenDateIsInThePast() {
        Event pastEvent = Event.builder()
                .date(OffsetDateTime.now().minusDays(1))
                .totalCapacity(100)
                .build();
        when(eventMapper.toEntity(requestDto)).thenReturn(pastEvent);

        assertThatThrownBy(() -> eventService.createEvent(requestDto, tenant))
                .isInstanceOf(PastEventException.class);

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an {@link IllegalArgumentException} is thrown when the
     * requested capacity exceeds the allowed maximum of 10 000, and that
     * no save is attempted.
     */
    @Test
    void createEvent_throwsIllegalArgumentException_whenCapacityExceeds10000() {
        Event oversizedEvent = Event.builder()
                .date(OffsetDateTime.now().plusDays(5))
                .totalCapacity(10001)
                .availableCapacity(10001)
                .build();
        when(eventMapper.toEntity(requestDto)).thenReturn(oversizedEvent);

        assertThatThrownBy(() -> eventService.createEvent(requestDto, tenant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10000");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that a capacity of exactly 10 000 is accepted as a valid
     * boundary value (no exception thrown, DTO returned successfully).
     */
    @Test
    void createEvent_allowsCapacityExactly10000() {
        Event maxEvent = Event.builder()
                .date(OffsetDateTime.now().plusDays(5))
                .totalCapacity(10000)
                .availableCapacity(10000)
                .build();
        EventResponseDto maxDto = EventResponseDto.builder()
                .id(UUID.randomUUID())
                .totalCapacity(10000)
                .availableCapacity(10000)
                .build();
        when(eventMapper.toEntity(requestDto)).thenReturn(maxEvent);
        when(eventRepository.save(maxEvent)).thenReturn(maxEvent);
        when(eventMapper.toDto(maxEvent)).thenReturn(maxDto);

        EventResponseDto result = eventService.createEvent(requestDto, tenant);

        assertThat(result.getTotalCapacity()).isEqualTo(10000);
    }


    /**
     * Verifies that a valid ID belonging to the given tenant returns the
     * expected response DTO.
     */
    @Test
    void getEventById_success_returnsEventResponseDto() {
        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(eventMapper.toDto(event)).thenReturn(responseDto);

        EventResponseDto result = eventService.getEventById(eventId, tenant);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(eventId);
    }

    /**
     * Verifies that a {@link ResourceNotFoundException} containing the event ID
     * is thrown when no event is found for the given ID and tenant.
     */
    @Test
    void getEventById_throwsResourceNotFoundException_whenEventNotFound() {
        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(eventId, tenant))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    /**
     * Verifies that the repository lookup is always scoped to the provided
     * tenant, preventing cross-tenant data leakage.
     */
    @Test
    void getEventById_scopesQueryByTenant() {
        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(eventMapper.toDto(any())).thenReturn(responseDto);

        eventService.getEventById(eventId, tenant);

        verify(eventRepository).findByIdAndTenant(eventId, tenant);
    }


    /**
     * Verifies that a valid update request returns a non-null response DTO.
     */
    @Test
    void updateEvent_success_returnsUpdatedDto() {
        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDto(event)).thenReturn(responseDto);

        EventResponseDto result = eventService.updateEvent(eventId, requestDto, tenant);

        assertThat(result).isNotNull();
    }

    /**
     * Verifies that the name, description, and location fields of the event
     * entity are overwritten with the values from the request DTO.
     */
    @Test
    void updateEvent_updatesNameDescriptionLocationDate() {
        EventRequestDto updatedDto = EventRequestDto.builder()
                .name("New Name")
                .description("New Desc")
                .location("Rotterdam")
                .date(OffsetDateTime.now().plusDays(20))
                .totalCapacity(200)
                .build();
        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);

        eventService.updateEvent(eventId, updatedDto, tenant);

        assertThat(event.getName()).isEqualTo("New Name");
        assertThat(event.getDescription()).isEqualTo("New Desc");
        assertThat(event.getLocation()).isEqualTo("Rotterdam");
    }

    /**
     * Verifies that a {@link ResourceNotFoundException} is thrown when the
     * event does not exist for the given tenant.
     */
    @Test
    void updateEvent_throwsResourceNotFoundException_whenEventNotFound() {
        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(eventId, requestDto, tenant))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    /**
     * Verifies that increasing the total capacity adjusts availableCapacity
     * correctly by preserving the number of already-booked seats.
     *
     * Example: 100 total, 60 available (40 booked) -> 150 total, 110 available.
     */
    @Test
    void updateEvent_increasingCapacity_updatesAvailableCapacityPreservingBookedSeats() {
        event.setTotalCapacity(100);
        event.setAvailableCapacity(60); // 40 seats are booked

        EventRequestDto increaseDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("Amsterdam")
                .date(OffsetDateTime.now().plusDays(10))
                .totalCapacity(150)
                .build();

        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);

        eventService.updateEvent(eventId, increaseDto, tenant);

        assertThat(event.getTotalCapacity()).isEqualTo(150);
        assertThat(event.getAvailableCapacity()).isEqualTo(110);
    }

    /**
     * Verifies that decreasing the total capacity is permitted when there are
     * no confirmed bookings, and that both capacity fields are updated accordingly.
     */
    @Test
    void updateEvent_decreasingCapacity_withNoActiveBookings_succeeds() {
        event.setTotalCapacity(200);
        event.setAvailableCapacity(200);

        EventRequestDto decreaseDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("Amsterdam")
                .date(OffsetDateTime.now().plusDays(10))
                .totalCapacity(100)
                .build();

        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(0);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);

        eventService.updateEvent(eventId, decreaseDto, tenant);

        assertThat(event.getTotalCapacity()).isEqualTo(100);
        assertThat(event.getAvailableCapacity()).isEqualTo(100);
    }

    /**
     * Verifies that an {@link EventDecreaseException} is thrown — and the
     * repository save is never called — when a capacity decrease is requested
     * while confirmed bookings exist. The exception message must include the
     * number of active bookings.
     */
    @Test
    void updateEvent_decreasingCapacity_throwsEventDecreaseException_whenActiveBookingsExist() {
        event.setTotalCapacity(200);

        EventRequestDto decreaseDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("Amsterdam")
                .date(OffsetDateTime.now().plusDays(10))
                .totalCapacity(50)
                .build();

        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(5);

        assertThatThrownBy(() -> eventService.updateEvent(eventId, decreaseDto, tenant))
                .isInstanceOf(EventDecreaseException.class)
                .hasMessageContaining("5");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that a {@link PastEventException} is thrown — and no save
     * occurs — when the new date supplied in the update request lies in the past.
     */
    @Test
    void updateEvent_throwsPastEventException_whenNewDateIsInThePast() {
        EventRequestDto pastDateDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("Amsterdam")
                .date(OffsetDateTime.now().minusDays(1))
                .totalCapacity(200)
                .build();

        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateEvent(eventId, pastDateDto, tenant))
                .isInstanceOf(PastEventException.class);

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an {@link IllegalArgumentException} is thrown — and no
     * save occurs — when the new capacity exceeds 10 000.
     */
    @Test
    void updateEvent_throwsIllegalArgumentException_whenNewCapacityExceeds10000() {
        EventRequestDto oversizedDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("Amsterdam")
                .date(OffsetDateTime.now().plusDays(10))
                .totalCapacity(10001)
                .build();

        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateEvent(eventId, oversizedDto, tenant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10000");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that email notifications are sent to every booked user when
     * the event date is changed. Each call should pass the user's email address,
     * the new date, and the (unchanged) location.
     */
    @Test
    void updateEvent_sendsEmailNotifications_whenDateChanges() {
        OffsetDateTime newDate = OffsetDateTime.now().plusDays(30);
        String originalLocation = event.getLocation();

        EventRequestDto changedDateDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location(originalLocation)
                .date(newDate)
                .totalCapacity(200)
                .build();

        User user1 = User.builder().id(UUID.randomUUID()).email("a@example.com").build();
        User user2 = User.builder().id(UUID.randomUUID()).email("b@example.com").build();
        Booking b1 = Booking.builder().user(user1).build();
        Booking b2 = Booking.builder().user(user2).build();

        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        when(bookingRepository.findByEventId(eventId)).thenReturn(List.of(b1, b2));

        eventService.updateEvent(eventId, changedDateDto, tenant);

        verify(emailService).sendEventUpdateEmail("a@example.com", newDate, originalLocation);
        verify(emailService).sendEventUpdateEmail("b@example.com", newDate, originalLocation);
    }

    /**
     * Verifies that email notifications are sent to every booked user when
     * the event location is changed. Each call should pass the user's email
     * address, the (unchanged) date, and the new location.
     */
    @Test
    void updateEvent_sendsEmailNotifications_whenLocationChanges() {
        OffsetDateTime originalDate = event.getDate();

        EventRequestDto changedLocationDto = EventRequestDto.builder()
                .name("Concert").description("Desc").location("Rotterdam")
                .date(originalDate)
                .totalCapacity(200)
                .build();

        User user1 = User.builder().id(UUID.randomUUID()).email("a@example.com").build();
        Booking b1 = Booking.builder().user(user1).build();

        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);
        when(bookingRepository.findByEventId(eventId)).thenReturn(List.of(b1));

        eventService.updateEvent(eventId, changedLocationDto, tenant);

        verify(emailService).sendEventUpdateEmail("a@example.com", originalDate, "Rotterdam");
    }

    /**
     * Verifies that no email notifications are sent — and the booking
     * repository is not queried — when neither the date nor the location
     * changes during an update.
     */
    @Test
    void updateEvent_doesNotSendEmail_whenNeitherDateNorLocationChanges() {
        EventRequestDto noChangeDto = EventRequestDto.builder()
                .name("New Name").description("New Desc")
                .location(event.getLocation())
                .date(event.getDate())
                .totalCapacity(200)
                .build();

        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(responseDto);

        eventService.updateEvent(eventId, noChangeDto, tenant);

        verify(emailService, never()).sendEventUpdateEmail(any(), any(), any());
        verify(bookingRepository, never()).findByEventId(any());
    }

    /**
     * Verifies that deleting an event performs a soft delete by setting its
     * status to {@link Status#CANCELLED} and saving the updated entity,
     * rather than removing the record from the database.
     */
    @Test
    void deleteEvent_success_softDeletesBySettingStatusToCancelled() {
        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(0);

        eventService.deleteEvent(eventId, tenant);

        assertThat(event.getStatus()).isEqualTo(Status.CANCELLED);
        verify(eventRepository).save(event);
    }

    /**
     * Verifies that a {@link ResourceNotFoundException} is thrown — and no
     * save occurs — when the event to delete is not found for the given tenant.
     */
    @Test
    void deleteEvent_throwsResourceNotFoundException_whenEventNotFound() {
        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.deleteEvent(eventId, tenant))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(eventId.toString());

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that an {@link EventDeleteException} is thrown — and no save
     * occurs — when confirmed bookings exist for the event. The exception
     * message must include the active booking count.
     */
    @Test
    void deleteEvent_throwsEventDeleteException_whenActiveBookingsExist() {
        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(4);

        assertThatThrownBy(() -> eventService.deleteEvent(eventId, tenant))
                .isInstanceOf(EventDeleteException.class)
                .hasMessageContaining("4");

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that a {@link PastEventException} is thrown — and no save
     * occurs — when an attempt is made to delete an event whose date has
     * already passed.
     */
    @Test
    void deleteEvent_throwsPastEventException_whenEventIsInThePast() {
        event.setDate(OffsetDateTime.now().minusDays(1));

        when(eventRepository.findByIdAndTenant(eventId, tenant)).thenReturn(Optional.of(event));
        when(bookingRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED)).thenReturn(0);

        assertThatThrownBy(() -> eventService.deleteEvent(eventId, tenant))
                .isInstanceOf(PastEventException.class);

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that the service returns a correctly mapped page of
     * {@link EventResponseDto} objects when events exist for the tenant.
     */
    @Test
    void getAllEvents_returnsPageOfEventResponseDtos() {
        Pageable pageable = PageRequest.of(0, 10); //first ten elements of page 0
        Page<Event> eventPage = new PageImpl<>(List.of(event)); //creates a fake page containing only the given event
        when(eventRepository.findByTenant(tenant, pageable)).thenReturn(eventPage);
        when(eventMapper.toDto(event)).thenReturn(responseDto);

        Page<EventResponseDto> result = eventService.getAllEvents(pageable, tenant);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(responseDto);
    }

    /**
     * Verifies that an empty page is returned without errors when no events
     * exist for the tenant.
     */
    @Test
    void getAllEvents_returnsEmptyPage_whenNoEventsExist() {
        Pageable pageable = PageRequest.of(0, 10);
        when(eventRepository.findByTenant(tenant, pageable)).thenReturn(Page.empty());

        Page<EventResponseDto> result = eventService.getAllEvents(pageable, tenant);

        assertThat(result).isEmpty();
    }

    /**
     * Verifies that the mapper is invoked once per event in the page, and
     * that each mapped DTO appears in the result in the correct order.
     */
    @Test
    void getAllEvents_mapsEachEventToDto() {
        Event event2 = Event.builder()
                .id(UUID.randomUUID()).name("Fest").date(OffsetDateTime.now().plusDays(5))
                .totalCapacity(50).availableCapacity(50).tenant(tenant).build();
        EventResponseDto dto2 = EventResponseDto.builder()
                .id(event2.getId()).name("Fest").build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(List.of(event, event2));
        when(eventRepository.findByTenant(tenant, pageable)).thenReturn(eventPage);
        when(eventMapper.toDto(event)).thenReturn(responseDto);
        when(eventMapper.toDto(event2)).thenReturn(dto2);

        Page<EventResponseDto> result = eventService.getAllEvents(pageable, tenant);

        assertThat(result.getContent()).containsExactly(responseDto, dto2);
        verify(eventMapper, times(2)).toDto(any(Event.class));
    }
}

