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

    private Venue venue;
    private User user;
    private User adminUser;
    private Event futureEvent;

    /**
     * Initializes the shared domain objects used across all tests.
     * Each field is a simple, valid instance; individual tests may
     * override specific properties to exercise edge cases.
     */
    @BeforeEach
    void initialize() {
        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("cinema")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("user@cinema.com")
                .role(Role.GUEST)
                .venue(venue)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@cinema.com")
                .role(Role.ADMIN)
                .venue(venue)
                .build();

        futureEvent = Event.builder()
                .id(UUID.randomUUID())
                .name("movie")
                .date(OffsetDateTime.now().plusDays(7))
                .bookingPolicy(new BookingPolicy(100,100))
                .venue(venue)
                .status(CONFIRMED)
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
        @DisplayName("createBooking_givenValidRequest_thenReturnBookingDto")
        void createBooking_givenValidRequest_thenReturnBookingDto() {

            int quantity = 2;
            BookingDto expectedDto = BookingDto.builder()
                    .eventId(futureEvent.getId())
                    .userId(user.getId())
                    .quantity(quantity)
                    .status(CONFIRMED)
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
                    futureEvent.getId(), user, user, venue, quantity, false);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CONFIRMED);
            assertThat(result.getQuantity()).isEqualTo(quantity);
            verify(bookingRepository).save(any(Booking.class));
        }

        /**
         * When the event does not exist (or belongs to a different venue),
         * the service must throw {@link ResourceNotFoundException}.
         */
        @Transactional
        @Test
        @DisplayName("createBooking_givenUnknownEvent_thenThrowResourceNotFoundException")
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
         * Attempting to book a quantity of zero must throw {@link QuantityException}
         * before any repository interaction.
         */
        @Transactional
        @Test
        @DisplayName("createBooking_givenZeroQuantity_thenThrowQuantityException")
        void createBooking_givenZeroQuantity_thenThrowQuantityException() {
            when(eventRepository.findByIdAndVenue(futureEvent.getId(), venue))
                    .thenReturn(Optional.of(futureEvent));

            assertThatThrownBy(() ->
                    bookingService.createBooking(
                            futureEvent.getId(), user, user, venue, 0, false))
                    .isInstanceOf(QuantityException.class);

            verify(bookingRepository, never()).save(any());
        }

        /**
         * When the event capacity is already full, {@code decreaseCapacity} returns 0
         * and the service must throw {@link NotEnoughSeatsException}.
         */
        @Transactional
        @Test
        @DisplayName("createBooking_givenFullCapacity_thenThrowNotEnoughSeatsException")
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
         * A user who already has a CONFIRMED booking for the same event must
         * receive a {@link BookingAlreadyPresentException}.
         */
        @Transactional
        @Test
        @DisplayName("createBooking_givenDuplicateBooking_thenThrowBookingAlreadyPresentException")
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
         * Booking a past event must throw {@link PastEventException} immediately.
         */
        @Transactional
        @Test
        @DisplayName("createBooking_givenPastEvent_thenThrowPastEventException")
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
         * When a user previously canceled their booking and re-books the same event,
         * the service should reactivate (update) the existing canceled record instead
         * of inserting a duplicate.
         */
        @Transactional
        @Test
        @DisplayName("createBooking_givenPreviouslyCancelledBooking_thenReactivateBooking")
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
    @DisplayName("cancelBooking")
    class CancelBooking {

        /**
         * Happy path: a user cancels their own booking. The service should
         * restore event capacity and call {@code deleteBooking} (soft-delete).
         */
        @Transactional
        @Test
        @DisplayName("cancelBooking_givenOwnerRequest_thenCancelAndRestoreCapacity")
        void cancelBooking_givenOwnerRequest_thenCancelAndRestoreCapacity() {

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

            verify(eventRepository).increaseCapacity(futureEvent.getId(), 2);
            verify(bookingRepository).deleteBooking(booking.getId());
        }

        /**
         * A non-admin user attempting to cancel another user's booking must
         * receive an {@link AccessDeniedException}.
         */
        @Transactional
        @Test
        @DisplayName("cancelBooking_givenNonOwnerNonAdmin_thenThrowAccessDeniedException")
        void cancelBooking_givenNonOwnerNonAdmin_thenThrowAccessDeniedException() {

            User otherUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("other@acme.com")
                    .venue(venue)
                    .build();

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(otherUser)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.cancelBooking(booking.getId(), user, venue, false))
                    .isInstanceOf(AccessDeniedException.class);

            verify(bookingRepository, never()).deleteBooking(any());
        }

        /**
         * An admin may cancel any booking within their venue, regardless of ownership.
         */
        @Transactional
        @Test
        @DisplayName("cancelBooking_givenAdminRequest_thenCancelSuccessfully")
        void cancelBooking_givenAdminRequest_thenCancelSuccessfully() {
            // arrange
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

            // act — adminUser cancels user's booking
            bookingService.cancelBooking(booking.getId(), adminUser, venue, true);

            // assert
            verify(bookingRepository).deleteBooking(booking.getId());
        }
    }


    @Nested
    @DisplayName("modifyQuantity")
    class ModifyQuantity {

        /**
         * Increasing the booking quantity succeeds when the event still has
         * enough remaining seats.
         */
        @Transactional
        @Test
        @DisplayName("modifyQuantity_givenValidIncrease_thenUpdateQuantity")
        void modifyQuantity_givenValidIncrease_thenUpdateQuantity() {

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(2)
                    .status(CONFIRMED)
                    .build();

            int newQuantity = 5;

            BookingDto expectedDto = BookingDto.builder()
                    .id(booking.getId())
                    .quantity(newQuantity)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));
            when(eventRepository.decreaseCapacity(futureEvent.getId(), newQuantity - 2))
                    .thenReturn(1);
            when(bookingRepository.save(booking)).thenReturn(booking);
            when(bookingMapper.toDto(booking)).thenReturn(expectedDto);

            BookingDto result = bookingService.modifyQuantity(
                    booking.getId(), user, venue, newQuantity, false);

            assertThat(result.getQuantity()).isEqualTo(newQuantity);
            verify(bookingRepository).save(booking);
        }

        /**
         * A non-admin user must not be able to modify another user's booking;
         * an {@link AccessDeniedException} is expected.
         */
        @Transactional
        @Test
        @DisplayName("modifyQuantity_givenNonOwnerNonAdmin_thenThrowAccessDeniedException")
        void modifyQuantity_givenNonOwnerNonAdmin_thenThrowAccessDeniedException() {

            User otherUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("other@acme.com")
                    .venue(venue)
                    .build();

            Booking booking = Booking.builder()
                    .id(UUID.randomUUID())
                    .user(otherUser)
                    .event(futureEvent)
                    .venue(venue)
                    .quantity(1)
                    .status(CONFIRMED)
                    .build();

            when(bookingRepository.findByIdAndVenue(booking.getId(), venue))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.modifyQuantity(
                            booking.getId(), user, venue, 3, false))
                    .isInstanceOf(AccessDeniedException.class);
        }

        /**
         * Requesting a quantity of zero must throw {@link QuantityException}
         * before any capacity check occurs.
         */
        @Transactional
        @Test
        @DisplayName("modifyQuantity_givenZeroQuantity_thenThrowQuantityException")
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

            verify(eventRepository, never()).decreaseCapacity(any(), anyInt());
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
        @DisplayName("getUserBookings_givenRegularUser_thenReturnOwnBookings")
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
        @DisplayName("getUserBookings_givenAdminRequest_thenReturnTargetUserBookings")
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
                    adminUser.getId(),
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
        @DisplayName("getUserBookings_givenUnknownUser_thenThrowResourceNotFoundException")
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