package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Booking;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Venue;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.BookingDto;
import com.razza.bookingsystem.exception.*;
import com.razza.bookingsystem.mapper.BookingMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;

import static com.razza.bookingsystem.domain.Status.CANCELLED;
import static com.razza.bookingsystem.domain.Status.CONFIRMED;

/**
 * Service responsible for managing bookings.
 *
 * Handles:
 *  Booking creation
 *  Booking modification
 *  Booking cancellation
 *  Retrieving bookings
 *
 * Enforces business rules such as seat availability,
 * venue isolation, and access control.
 */
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final BookingMapper bookingMapper;
    private final UserRepository userRepository;

    /**
     * Creates a new booking for a given event.
     *
     * Behavior:
     *  Validates that the event exists within the venue
     *  Checks that the event is not in the past
     *  Checks that the event requires bookings and that the quantity is valid
     *  Allows admins to book on behalf of another user within the same venue
     *  Checks the age restriction of the event
     *  Prevents duplicate bookings for the same event and user
     *  Decreases the available event capacity
     *  Reactivates a previously cancelled booking, or creates a new one
     *  Persists the booking
     *
     * @param eventId identifier of the event
     * @param user target user for the booking (used when admin books on behalf of someone)
     * @param currentUser authenticated user performing the request
     * @param venue venue of the requesting user
     * @param quantity number of seats requested
     * @param isAdmin whether the current user has admin privileges
     * @return the created booking as a {@link BookingDto}
     * @throws ResourceNotFoundException if the event or user does not exist or does not belong to the venue
     * @throws PastEventException if the event is already in the past
     * @throws QuantityException if the event does not require bookings or the quantity is less than 1
     * @throws invalidAgeException if the event is 18 plus and the user is younger
     * @throws NotEnoughSeatsException if there is insufficient capacity
     * @throws BookingAlreadyPresentException if a booking already exists for the same user and event
     */
    @Transactional
    public BookingDto createBooking(UUID eventId, User user, User currentUser, Venue venue, int quantity, Boolean isAdmin) {

        Event event = eventRepository.findByIdAndVenue(eventId, currentUser.getVenue())
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        if(event.getBookingPolicy() == null ){
            throw new QuantityException();
        }

        if(quantity < 1){
            throw new QuantityException(quantity);
        }

        if (isAdmin) {
            currentUser = user;

            if (!user.getVenue().getId().equals(venue.getId())) {
                throw new ResourceNotFoundException("user");
            }
        }

        if(Boolean.TRUE.equals(event.getEighteenPlus()) && ChronoUnit.YEARS.between(currentUser.getBirthDate(), OffsetDateTime.now()) < 18) {            throw new invalidAgeException();
        }

        var existingBooking = bookingRepository.findByUserAndEventAndStatus(currentUser, event, CONFIRMED);
        var existingCancelledBooking = bookingRepository.findByUserAndEventAndStatus(currentUser, event, CANCELLED);

        if (existingBooking.isPresent()) {
            throw new BookingAlreadyPresentException(existingBooking.get().getQuantity());
        }

        int result = eventRepository.decreaseCapacity(eventId, quantity);

        if(result == 0){
            throw new NotEnoughSeatsException(event.getBookingPolicy().getAvailableCapacity());
        }

        if (existingCancelledBooking.isPresent()) {
            Booking booking = existingCancelledBooking.get();
            booking.setStatus(CONFIRMED);
            booking.setQuantity(quantity);
            booking.setCreatedAt(OffsetDateTime.now());
            Booking saved = bookingRepository.save(booking);
            return bookingMapper.toDto(saved);
        }

        Booking booking = Booking.builder()
                .user(currentUser)
                .event(event)
                .venue(event.getVenue())
                .quantity(quantity)
                .status(com.razza.bookingsystem.domain.Status.CONFIRMED)
                .createdAt(OffsetDateTime.now())
                .build();

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toDto(saved);
    }

    /**
     * Modifies the quantity of an existing booking.
     *
     * Behavior:
     *  Validates that the booking exists within the venue
     *  Checks that the event is not in the past
     *  Enforces ownership or admin access
     *  Enforces venue isolation
     *  Checks that the new quantity is valid
     *  Updates the event capacity, failing if additional seats are not available
     *  Persists the updated booking
     *
     * @param bookingId identifier of the booking
     * @param user authenticated user performing the request
     * @param userVenue venue of the requesting user
     * @param quantity new desired quantity
     * @param isAdmin whether the user has admin privileges
     * @return the updated booking as a {@link BookingDto}
     * @throws ResourceNotFoundException if the booking does not exist or does not belong to the venue
     * @throws PastEventException if the event is already in the past
     * @throws AccessDeniedException if the user is not allowed to modify the booking
     * @throws QuantityException if the new quantity is less than 1
     * @throws NotEnoughSeatsException if there is insufficient capacity for the requested increase
     */
    @Transactional
    public BookingDto modifyQuantity(UUID bookingId, User user, Venue userVenue, int quantity, Boolean isAdmin) {

        Booking booking = bookingRepository.findByIdAndVenue(bookingId, userVenue)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        Event event = booking.getEvent();

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        if (!isAdmin && !user.equals(booking.getUser())) {
            throw new AccessDeniedException("Not allowed to modify this booking");
        }

        if (!booking.getEvent().getVenue().getId().equals(userVenue.getId())) {
            throw new ResourceNotFoundException("booking", bookingId);
        }

        if(quantity < 1){
            throw new QuantityException(quantity);
        }

        int result = eventRepository.decreaseCapacity(event.getId(), quantity - booking.getQuantity());

        if(result == 0){
            throw new NotEnoughSeatsException(event.getBookingPolicy().getAvailableCapacity(), booking.getQuantity());
        }

        booking.setQuantity(quantity);

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toDto(saved);
    }

    /**
     * Cancels an existing booking.
     *
     * Behavior:
     *  Validates that the booking exists within the venue
     *  Prevents duplicate cancellation
     *  Checks that the event is not in the past
     *  Enforces ownership or admin access
     *  Restores event capacity
     *  Marks the booking as canceled
     *
     * @param bookingId identifier of the booking
     * @param currentUser authenticated user performing the request
     * @param venue venue of the requesting user
     * @param isAdmin whether the user has admin privileges
     * @throws ResourceNotFoundException if the booking does not exist within the venue
     * @throws BookingAlreadyCancelledException if the booking is already canceled
     * @throws PastEventException if the event is already in the past
     * @throws AccessDeniedException if the user is not allowed to cancel the booking
     */
    @Transactional
    public void cancelBooking(UUID bookingId, User currentUser, Venue venue, boolean isAdmin) {

        Booking booking = bookingRepository.findByIdAndVenue(bookingId, venue)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if(booking.getStatus().equals(CANCELLED)){
            throw new BookingAlreadyCancelledException();
        }

        Event event = booking.getEvent();

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        if (!isAdmin && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        if (!booking.getVenue().getId().equals(venue.getId())) {
            throw new ResourceNotFoundException("booking", bookingId);
        }

        eventRepository.increaseCapacity(event.getId(), booking.getQuantity());
        bookingRepository.deleteBooking(bookingId);
    }

    /**
     * Retrieves all bookings for a specific user within a venue.
     *
     * Behavior:
     *  Regular users can only access their own bookings
     *  Admin users can access bookings of any user within the venue
     *  Validates that the user exists within the venue
     *  Maps results to DTOs
     *
     * @param userId identifier of the target user
     * @param currentUserId identifier of the requesting user
     * @param venue venue of the requesting user
     * @param isAdmin whether the user has admin privileges
     * @return list of bookings as {@link BookingDto}
     * @throws ResourceNotFoundException if the user does not exist within the venue
     */
    public List<BookingDto> getUserBookings(UUID userId, UUID currentUserId, Venue venue, boolean isAdmin) {

        if (!isAdmin){
            userId = currentUserId;
        }

        if(userRepository.findByIdAndVenueId(userId, venue.getId()).isEmpty()){
            throw new ResourceNotFoundException("user", userId);
        }

        return bookingRepository.findByUserIdAndVenue(userId, venue)
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }
}