package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.BookingDto;
import com.razza.bookingsystem.exception.*;
import com.razza.bookingsystem.mapper.BookingMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.razza.bookingsystem.domain.Status.CANCELLED;
import static com.razza.bookingsystem.domain.Status.CONFIRMED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingService}.
 *
 * Each nested class covers one public method of the service.
 * Mocks replace all repository and mapper dependencies so tests
 * run without a database.
 *
 * Naming convention for test methods:
 *   methodName_givenCondition_thenExpectedOutcome
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private Venue venue, otherVenue;
    private User user, otherUser, minorUser, admin, performer;
    private Event futureEvent, secondFutureEvent;
    private Performance performance;

    /**
     * Initializes the shared domain objects used across all tests.
     * Each field is a simple, valid instance; individual tests may
     * override specific properties to exercise edge cases.
     */
    @BeforeEach
    void initialize() {
        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("music club")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .name("user")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .email("user@example.com")
                .password("hashedPassword")
                .role(Role.GUEST)
                .venue(venue)
                .status(Status.CONFIRMED)
                .build();

        performer = User.builder()
                .id(UUID.randomUUID())
                .name("performer")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .email("performer@example.com")
                .password("hashedPassword")
                .role(Role.PERFORMER)
                .venue(venue)
                .status(Status.CONFIRMED)
                .build();

        admin = User.builder()
                .id(UUID.randomUUID())
                .name("admin")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .email("admin@example.com")
                .password("hashedPassword")
                .role(Role.ADMIN)
                .venue(venue)
                .status(Status.CONFIRMED)
                .build();

        performance = Performance.builder()
                .id(UUID.randomUUID())
                .user(performer)
                .name("blues")
                .description("blues session")
                .duration(120)
                .performanceType(PerformanceType.LIVE_MUSIC)
                .build();

        Venue otherVenue = Venue.builder()
                .id(UUID.randomUUID())
                .name("other music club")
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .name("otherUser")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .email("otherUserser@example.com")
                .password("hashedPassword")
                .role(Role.GUEST)
                .venue(otherVenue)
                .status(Status.CONFIRMED)
                .build();

        minorUser = User.builder()
                .id(UUID.randomUUID())
                .name("minorUser")
                .birthDate(OffsetDateTime.parse("2010-01-01T00:00:00Z"))
                .email("otherUserser@example.com")
                .password("hashedPassword")
                .role(Role.GUEST)
                .venue(venue)
                .status(Status.CONFIRMED)
                .build();

        futureEvent = Event.builder()
                .id(UUID.randomUUID())
                .name("cabaret")
                .description("a fun cabaret")
                .location("main room")
                .date(OffsetDateTime.now().plusDays(7))
                .bookingPolicy(new BookingPolicy(100,100))
                .venue(venue)
                .status(CONFIRMED)
                .schedule(List.of(new TimeSlot(performer.getEmail(), OffsetDateTime.now().plusDays(7), OffsetDateTime.now().plusDays(7).plusMinutes(120), performance.getId())))
                .eighteenPlus(false)
                .build();

        secondFutureEvent = Event.builder()
                .id(UUID.randomUUID())
                .name("second cabaret")
                .description("an other fun cabaret")
                .location("the second room")
                .date(OffsetDateTime.now().plusDays(7))
                .bookingPolicy(new BookingPolicy(100,100))
                .venue(venue)
                .status(CONFIRMED)
                .schedule(List.of(new TimeSlot(performer.getEmail(), OffsetDateTime.now().plusDays(7), OffsetDateTime.now().plusDays(7).plusMinutes(120), performance.getId())))
                .eighteenPlus(true)
                .build();
    }

    @Nested
    @DisplayName("createBooking")
    class CreateBooking {

        /**
         * Happy path: a regular user creates a new booking for a future event
         * with sufficient capacity. The service should persist the booking and
         * return a mapped DTO.
         */
        @Transactional
        @Test
        void createBookingByUser_givenValidRequest_thenReturnBookingDto() {

            int quantity = 2;
            BookingDto expectedDto = BookingDto.builder()
                    .eventId(futureEvent.getId())
                    .eventName("cabaret")
                    .userId(user.getId())
                    .quantity(quantity)
                    .status(CONFIRMED)
                    .createdAt(OffsetDateTime.now())
                    .build();

            when(eventRepository.findByIdAndVenue(futureEvent.getId(), venue))
                    .thenReturn(Optional.of(futureEvent));
            when(bookingRepository.findByUserAndEventAndStatus(user, futureEvent, CONFIRMED))
                    .thenReturn(Optional.empty());
            when(bookingRepository.findByUserAndEventAndStatus(user, futureEvent, CANCELLED))
                    .thenReturn(Optional.empty());
            when(eventRepository.decreaseCapacity(futureEvent.getId(), quantity))
                    .thenReturn(1);
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0)); // thenAnswer is used instead of thenReturn because the object (Booking) will only exist at runtime, inv (invocationOnMock is instead a snapshot of the saving operation, inv.getArgument(0) is nothing else that the booking itself
            when(bookingMapper.toDto(any(Booking.class)))
                    .thenReturn(expectedDto);

            BookingDto result = bookingService.createBooking(
                    futureEvent.getId(), null, user, venue, quantity, false);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CONFIRMED);
            assertThat(result.getQuantity()).isEqualTo(quantity);
            verify(bookingRepository).save(any(Booking.class));
        }

        @Transactional
        @Test
        void createBookingByAdmin_givenValidRequest_thenReturnBookingDto() {

            int quantity = 2;
            BookingDto expectedDto = BookingDto.builder()
                    .eventId(futureEvent.getId())
                    .eventName("cabaret")
                    .userId(user.getId())
                    .quantity(quantity)
                    .status(CONFIRMED)
                    .createdAt(OffsetDateTime.now())
                    .build();

            when(eventRepository.findByIdAndVenue(futureEvent.getId(), venue))
                    .thenReturn(Optional.of(futureEvent));
            when(bookingRepository.findByUserAndEventAndStatus(user, futureEvent, CONFIRMED))
                    .thenReturn(Optional.empty());
            when(bookingRepository.findByUserAndEventAndStatus(user, futureEvent, CANCELLED))
                    .thenReturn(Optional.empty());
            when(eventRepository.decreaseCapacity(futureEvent.getId(), quantity))
                    .thenReturn(1);
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0)); // thenAnswer is used instead of thenReturn because the object (Booking) will only exist at runtime, inv (invocationOnMock is instead a snapshot of the saving operation, inv.getArgument(0) is nothing else that the booking itself
            when(bookingMapper.toDto(any(Booking.class)))
                    .thenReturn(expectedDto);

            BookingDto result = bookingService.createBooking(
                    futureEvent.getId(), user, admin, venue, quantity, true);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CONFIRMED);
            assertThat(result.getQuantity()).isEqualTo(quantity);
            assertThat(result.getUserId()).isEqualTo(user.getId());
            verify(bookingRepository).save(any(Booking.class));
        }


        /**
         * When the event does not exist (or belongs to a different venue),
         * the service must throw {@link ResourceNotFoundException}.
         */
        @Transactional
        @Test
        void createBooking_givenUnknownEvent_thenThrowResourceNotFoundException() {

            when(eventRepository.findByIdAndVenue(any(UUID.class), any(Venue.class)))
                    .thenReturn(Optional.empty());  //number of when clauses is variable, only the mocks that the execution will go through should be stubbed, in this case, since the event check is done at the beginning of createBooking, there is no need to include other mocks

            assertThatThrownBy(() ->        // lambda syntax is used so that the createBooking() is not run instantly, but it is given to assertThrownBy to verify the kind of exception it throws
                    bookingService.createBooking(
                            UUID.randomUUID(), user, user, venue, 1, false))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(bookingRepository, never()).save(any()); // this is just the common used syntax: verify(mock, times(1)).method()
        }

        /**
         * Booking a past event must throw {@link PastEventException} immediately.
         */
        @Transactional
        @Test
        void createBooking_givenPastEvent_thenThrowPastEventException() {

            Event pastEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .name("Old Concert")
                    .date(OffsetDateTime.now().minusDays(1))
                    .bookingPolicy(new BookingPolicy(50,50))
                    .venue(venue)
                    .status(CONFIRMED)
                    .build();

            when(eventRepository.findByIdAndVenue(pastEvent.getId(), venue))
                    .thenReturn(Optional.of(pastEvent));

            assertThatThrownBy(() ->
                    bookingService.createBooking(
                            pastEvent.getId(), user, user, venue, 1, false))
                    .isInstanceOf(PastEventException.class);
        }

        /**
         * Attempting to book a quantity of zero must throw {@link QuantityException}
         * before any repository interaction.
         */
        @Transactional
        @Test
        void createBooking_givenNoBookingPolicy_thenThrowQuantityException() {
            futureEvent.setBookingPolicy(null);

            when(eventRepository.findByIdAndVenue(futureEvent.getId(), venue))
                    .thenReturn(Optional.of(futureEvent));

            assertThatThrownBy(() ->
                    bookingService.createBooking(
                            futureEvent.getId(), user, user, venue, 2, false))
                    .isInstanceOf(QuantityException.class)
                    .hasMessageContaining("This event does not require any bookings");

            verify(bookingRepository, never()).save(any());
        }

        /**
         * Attempting to book a quantity of zero must throw {@link QuantityException}
         * before any repository interaction.
         */
        @Transactional
        @Test
        void createBooking_givenZeroQuantity_thenThrowQuantityException() {
            when(eventRepository.findByIdAndVenue(futureEvent.getId(), venue))
                    .thenReturn(Optional.of(futureEvent));

            assertThatThrownBy(() ->
                    bookingService.createBooking(
                            futureEvent.getId(), user, user, venue, 0, false))
                    .isInstanceOf(QuantityException.class);

            verify(bookingRepository, never()).save(any());
        }

        @Transactional
        @Test
        void createBookingByAdmin_givenWrongVenue_thenThrowResourceNotFoundException() {
            when(eventRepository.findByIdAndVenue(futureEvent.getId(), venue))
                    .thenReturn(Optional.of(futureEvent));

            assertThatThrownBy(() ->
                    bookingService.createBooking(
                            futureEvent.getId(), otherUser, admin, venue, 2, true))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(bookingRepository, never()).save(any());
        }

        @Transactional
        @Test
        void createBooking_givenInvalidAge_thenThrowInvalidAgeException() {
            when(eventRepository.findByIdAndVenue(secondFutureEvent.getId(), venue))
                    .thenReturn(Optional.of(secondFutureEvent));

            assertThatThrownBy(() ->
                    bookingService.createBooking(
                            secondFutureEvent.getId(), minorUser, admin, venue, 2, true))
                    .isInstanceOf(invalidAgeException.class);

            verify(bookingRepository, never()).save(any());
        }


        /**
         * A user who already has a CONFIRMED booking for the same event must
         * receive a {@link BookingAlreadyPresentException}.
         */
        @Transactional
        @Test
        void createBooking_givenDuplicateBooking_thenThrowBookingAlreadyPresentException() {

            Booking existing = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .quantity(2)
                    .status(CONFIRMED)
                    .build();

            when(eventRepository.findByIdAndVenue(futureEvent.getId(), venue))
                    .thenReturn(Optional.of(futureEvent));
            when(bookingRepository.findByUserAndEventAndStatus(user, futureEvent, CONFIRMED))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() ->
                    bookingService.createBooking(
                            futureEvent.getId(), user, user, venue, 1, false))
                    .isInstanceOf(BookingAlreadyPresentException.class);
        }

        /**
         * When the event capacity is already full, {@code decreaseCapacity} returns 0
         * and the service must throw {@link NotEnoughSeatsException}.
         */
        @Transactional
        @Test
        void createBooking_givenFullCapacity_thenThrowNotEnoughSeatsException() {

            when(eventRepository.findByIdAndVenue(futureEvent.getId(), venue))
                    .thenReturn(Optional.of(futureEvent));
            when(bookingRepository.findByUserAndEventAndStatus(user, futureEvent, CONFIRMED))
                    .thenReturn(Optional.empty());
            when(bookingRepository.findByUserAndEventAndStatus(user, futureEvent, CANCELLED))
                    .thenReturn(Optional.empty());
            when(eventRepository.decreaseCapacity(futureEvent.getId(), 5))
                    .thenReturn(0);     //unit testing is deliberately narrow and fast, here just testing how the method reacts to decreaseCapacity returning 0

            assertThatThrownBy(() ->
                    bookingService.createBooking(
                            futureEvent.getId(), user, user, venue, 5, false))
                    .isInstanceOf(NotEnoughSeatsException.class);
        }

        /**
         * When a user previously canceled their booking and re-books the same event,
         * the service should reactivate (update) the existing canceled record instead
         * of inserting a duplicate.
         */
        @Transactional
        @Test
        void createBooking_givenPreviouslyCancelledBooking_thenReactivateBooking() {

            Booking cancelled = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .quantity(1)
                    .status(CANCELLED)
                    .build();

            when(eventRepository.findByIdAndVenue(futureEvent.getId(), venue))
                    .thenReturn(Optional.of(futureEvent));
            when(bookingRepository.findByUserAndEventAndStatus(user, futureEvent, CONFIRMED))
                    .thenReturn(Optional.empty());
            when(bookingRepository.findByUserAndEventAndStatus(user, futureEvent, CANCELLED))
                    .thenReturn(Optional.of(cancelled));
            when(eventRepository.decreaseCapacity(futureEvent.getId(), 3))
                    .thenReturn(1);
            when(bookingRepository.save(cancelled)).thenReturn(cancelled);
            when(bookingMapper.toDto(cancelled)).thenReturn(new BookingDto());

            bookingService.createBooking(
                    futureEvent.getId(), user, user, venue, 3, false);

            assertThat(cancelled.getStatus()).isEqualTo(CONFIRMED);
            assertThat(cancelled.getQuantity()).isEqualTo(3);
            verify(bookingRepository).save(cancelled);
        }
    }


    @Nested
    @DisplayName("modifyQuantity")
    class ModifyQuantity {

        /**
         * Happy path: the booking owner increases the quantity of their own
         * booking, and there is enough capacity. The service should update
         * the booking and return a mapped DTO.
         */
        @Transactional
        @Test
        void modifyQuantity_givenValidRequestByOwner_thenReturnBookingDto() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(2)
                    .status(CONFIRMED)
                    .build();

            int newQuantity = 4;
            BookingDto expectedDto = BookingDto.builder()
                    .eventId(futureEvent.getId())
                    .userId(user.getId())
                    .quantity(newQuantity)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));
            when(eventRepository.decreaseCapacity(futureEvent.getId(), newQuantity - booking.getQuantity()))
                    .thenReturn(1);
            when(bookingRepository.save(booking))
                    .thenReturn(booking);
            when(bookingMapper.toDto(booking))
                    .thenReturn(expectedDto);

            BookingDto result = bookingService.modifyQuantity(
                    booking.getId(), user, venue, newQuantity, false);

            assertThat(result).isNotNull();
            assertThat(result.getQuantity()).isEqualTo(newQuantity);
            assertThat(booking.getQuantity()).isEqualTo(newQuantity);
            verify(bookingRepository).save(booking);
        }

        /**
         * Happy path: an admin modifies a booking that does not belong to
         * them, on behalf of another user within the same venue.
         */
        @Transactional
        @Test
        void modifyQuantity_givenValidRequestByAdmin_thenReturnBookingDto() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(2)
                    .status(CONFIRMED)
                    .build();

            int newQuantity = 3;
            BookingDto expectedDto = BookingDto.builder()
                    .eventId(futureEvent.getId())
                    .userId(user.getId())
                    .quantity(newQuantity)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));
            when(eventRepository.decreaseCapacity(futureEvent.getId(), newQuantity - booking.getQuantity()))
                    .thenReturn(1);
            when(bookingRepository.save(booking))
                    .thenReturn(booking);
            when(bookingMapper.toDto(booking))
                    .thenReturn(expectedDto);

            BookingDto result = bookingService.modifyQuantity(
                    booking.getId(), admin, venue, newQuantity, true);

            assertThat(result).isNotNull();
            assertThat(result.getQuantity()).isEqualTo(newQuantity);
            verify(bookingRepository).save(booking);
        }

        /**
         * When the booking does not exist (or does not belong to the venue),
         * the service must throw ResourceNotFoundException.
         */
        @Transactional
        @Test
        void modifyQuantity_givenUnknownBooking_thenThrowResourceNotFoundException() {

            when(bookingRepository.findByIdAndVenue(any(UUID.class), any(Venue.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    bookingService.modifyQuantity(
                            UUID.randomUUID(), user, venue, 2, false))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(bookingRepository, never()).save(any());
        }

        /**
         * Modifying a booking for a past event must throw PastEventException.
         */
        @Transactional
        @Test
        void modifyQuantity_givenPastEvent_thenThrowPastEventException() {

            Event pastEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .name("Old Concert")
                    .date(OffsetDateTime.now().minusDays(1))
                    .bookingPolicy(new BookingPolicy(50, 50))
                    .venue(venue)
                    .status(CONFIRMED)
                    .build();

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(pastEvent)
                    .venue(venue)
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.modifyQuantity(
                            booking.getId(), user, venue, 2, false))
                    .isInstanceOf(PastEventException.class);

            verify(bookingRepository, never()).save(any());
        }

        /**
         * A non-admin user attempting to modify someone else's booking
         * must receive an AccessDeniedException.
         */
        @Transactional
        @Test
        void modifyQuantity_givenNotOwnerAndNotAdmin_thenThrowAccessDeniedException() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(2)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.modifyQuantity(
                            booking.getId(), otherUser, venue, 2, false))
                    .isInstanceOf(AccessDeniedException.class);

            verify(bookingRepository, never()).save(any());
        }

        /**
         * When the booking's event belongs to a different venue than the
         * requesting user's venue, the service must throw
         * ResourceNotFoundException.
         */
        @Transactional
        @Test
        void modifyQuantity_givenWrongVenue_thenThrowResourceNotFoundException() {

            Event otherVenueEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .name("other venue event")
                    .date(OffsetDateTime.now().plusDays(7))
                    .bookingPolicy(new BookingPolicy(50, 50))
                    .venue(otherUser.getVenue())
                    .status(CONFIRMED)
                    .build();

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(otherVenueEvent)
                    .venue(otherUser.getVenue())
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.modifyQuantity(
                            booking.getId(), user, venue, 2, true))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(bookingRepository, never()).save(any());
        }

        /**
         * Requesting a quantity below one must throw QuantityException
         * before any capacity check is performed.
         */
        @Transactional
        @Test
        void modifyQuantity_givenZeroQuantity_thenThrowQuantityException() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(2)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.modifyQuantity(
                            booking.getId(), user, venue, 0, false))
                    .isInstanceOf(QuantityException.class);

            verify(bookingRepository, never()).save(any());
        }

        /**
         * When there is not enough remaining capacity to cover the increase
         * in quantity, decreaseCapacity returns 0 and the service must
         * throw NotEnoughSeatsException.
         */
        @Transactional
        @Test
        void modifyQuantity_givenNotEnoughSeats_thenThrowNotEnoughSeatsException() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(2)
                    .status(CONFIRMED)
                    .build();

            int newQuantity = 10;

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));
            when(eventRepository.decreaseCapacity(futureEvent.getId(), newQuantity - booking.getQuantity()))
                    .thenReturn(0);

            assertThatThrownBy(() ->
                    bookingService.modifyQuantity(
                            booking.getId(), user, venue, newQuantity, false))
                    .isInstanceOf(NotEnoughSeatsException.class);

            verify(bookingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        /**
         * Happy path: the booking owner cancels their own booking for a
         * future event. The service should restore event capacity and
         * delete the booking.
         */
        @Transactional
        @Test
        void cancelBooking_givenValidRequestByOwner_thenDeleteBookingAndRestoreCapacity() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(2)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            bookingService.cancelBooking(booking.getId(), user, venue, false);

            verify(eventRepository).increaseCapacity(futureEvent.getId(), booking.getQuantity());
            verify(bookingRepository).deleteBooking(booking.getId());
        }

        /**
         * Happy path: an admin cancels a booking that does not belong to
         * them, on behalf of another user within the same venue.
         */
        @Transactional
        @Test
        void cancelBooking_givenValidRequestByAdmin_thenDeleteBookingAndRestoreCapacity() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            bookingService.cancelBooking(booking.getId(), admin, venue, true);

            verify(eventRepository).increaseCapacity(futureEvent.getId(), booking.getQuantity());
            verify(bookingRepository).deleteBooking(booking.getId());
        }

        /**
         * When the booking does not exist (or does not belong to the venue),
         * the service must throw ResourceNotFoundException.
         */
        @Transactional
        @Test
        void cancelBooking_givenUnknownBooking_thenThrowResourceNotFoundException() {

            when(bookingRepository.findByIdAndVenue(any(UUID.class), any(Venue.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    bookingService.cancelBooking(
                            UUID.randomUUID(), user, venue, false))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(bookingRepository, never()).deleteBooking(any());
            verify(eventRepository, never()).increaseCapacity(any(), anyInt());
        }

        /**
         * Cancelling a booking that is already canceled must throw
         * BookingAlreadyCancelledException.
         */
        @Transactional
        @Test
        void cancelBooking_givenAlreadyCancelledBooking_thenThrowBookingAlreadyCancelledException() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(1)
                    .status(CANCELLED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.cancelBooking(
                            booking.getId(), user, venue, false))
                    .isInstanceOf(BookingAlreadyCancelledException.class);

            verify(bookingRepository, never()).deleteBooking(any());
            verify(eventRepository, never()).increaseCapacity(any(), anyInt());
        }

        /**
         * Cancelling a booking for a past event must throw
         * PastEventException.
         */
        @Transactional
        @Test
        void cancelBooking_givenPastEvent_thenThrowPastEventException() {

            Event pastEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .name("Old Concert")
                    .date(OffsetDateTime.now().minusDays(1))
                    .bookingPolicy(new BookingPolicy(50, 50))
                    .venue(venue)
                    .status(CONFIRMED)
                    .build();

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(pastEvent)
                    .venue(venue)
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.cancelBooking(
                            booking.getId(), user, venue, false))
                    .isInstanceOf(PastEventException.class);

            verify(bookingRepository, never()).deleteBooking(any());
        }

        /**
         * A non-admin user attempting to cancel someone else's booking
         * must receive an AccessDeniedException.
         */
        @Transactional
        @Test
        void cancelBooking_givenNotOwnerAndNotAdmin_thenThrowAccessDeniedException() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.cancelBooking(
                            booking.getId(), otherUser, venue, false))
                    .isInstanceOf(AccessDeniedException.class);

            verify(bookingRepository, never()).deleteBooking(any());
        }

        /**
         * When the booking belongs to a different venue than the requesting
         * user's venue, the service must throw ResourceNotFoundException.
         */
        @Transactional
        @Test
        void cancelBooking_givenWrongVenue_thenThrowResourceNotFoundException() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(otherUser.getVenue())
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.cancelBooking(
                            booking.getId(), user, venue, true))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(bookingRepository, never()).deleteBooking(any());
        }
    }


    @Nested
    @DisplayName("getUserBookings")
    class GetUserBookings {

        /**
         * A regular user can retrieve their own booking list. The method
         * ignores the supplied {@code userId} and always uses their own ID.
         */
        @Transactional
        @Test
        void getUserBookings_givenRegularUser_thenReturnOwnBookings() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            BookingDto dto = BookingDto.builder()
                    .id(booking.getId())
                    .userId(user.getId())
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            when(userRepository.findByIdAndVenueId(user.getId(), venue.getId()))
                    .thenReturn(Optional.of(user));
            when(bookingRepository.findByUserIdAndVenue(user.getId(), venue))
                    .thenReturn(List.of(booking));
            when(bookingMapper.toDto(booking)).thenReturn(dto);

            List<BookingDto> results = bookingService.getUserBookings(
                    UUID.randomUUID(),
                    user.getId(),
                    venue,
                    false);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getUserId()).isEqualTo(user.getId()); // even if a random UUID was passed to getUserBookings, the booking belonging to the user is returned, this is because the ID field is ignored when the authenticated user is not an admin
        }

        /**
         * An admin can retrieve bookings for any user within their venue
         * by supplying that user's ID.
         */
        @Transactional
        @Test
        void getUserBookings_givenAdminRequest_thenReturnTargetUserBookings() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(3)
                    .status(CONFIRMED)
                    .build();

            BookingDto dto = BookingDto.builder()
                    .id(booking.getId())
                    .userId(user.getId())
                    .quantity(3)
                    .status(CONFIRMED)
                    .build();

            when(userRepository.findByIdAndVenueId(user.getId(), venue.getId()))
                    .thenReturn(Optional.of(user));
            when(bookingRepository.findByUserIdAndVenue(user.getId(), venue))
                    .thenReturn(List.of(booking));
            when(bookingMapper.toDto(booking)).thenReturn(dto);

            List<BookingDto> results = bookingService.getUserBookings(
                    user.getId(),
                    admin.getId(),
                    venue,
                    true);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getQuantity()).isEqualTo(3);  //here the test asserts on quantity instead, verifying that the admin can correctly fetch users bookings within the same venue
        }

        /**
         * When the target user does not exist within the venue, the service
         * must throw {@link ResourceNotFoundException}.
         */
        @Transactional
        @Test
        void getUserBookings_givenUnknownUser_thenThrowResourceNotFoundException() {

            UUID unknownUserId = UUID.randomUUID();

            when(userRepository.findByIdAndVenueId(unknownUserId, venue.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    bookingService.getUserBookings(
                            unknownUserId, unknownUserId, venue, true))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}