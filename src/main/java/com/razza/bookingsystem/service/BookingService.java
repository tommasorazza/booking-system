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
 * - booking creation
 * - booking modification
 * - booking cancellation
 * - retrieving bookings
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
     * - validates that the event exists
     * - enforces venue isolation
     * - checks seat availability
     * - allows admins to book on behalf of another user within the same venue
     * - prevents duplicate bookings for the same event and user
     * - decreases available event capacity
     * - persists the booking
     *
     * @param eventId identifier of the event
     * @param user target user for the booking (used when admin books on behalf of someone)
     * @param currentUser authenticated user performing the request
     * @param venue venue of the requesting user
     * @param quantity number of seats requested
     * @param isAdmin whether the current user has admin privileges
     * @return the created booking as a {@link BookingDto}
     * @throws ResourceNotFoundException if the event or user does not exist or does not belong to the venue
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

        if (!event.getVenue().getId().equals(venue.getId())) {
            throw new ResourceNotFoundException("event", eventId);
        }

        if(event.getBookingPolicy() == null ){
            throw new IllegalStateException("This event doesn't require any booking");
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

        if(event.getEighteenPlus() && ChronoUnit.YEARS.between(currentUser.getBirthDate(), OffsetDateTime.now()) < 18) {
            throw new invalidAgeException();
        }

        var existingBooking = bookingRepository.findByUserAndEventAndStatus(currentUser, event, CONFIRMED);
        var existingCancelledBooking = bookingRepository.findByUserAndEventAndStatus(currentUser, event, CANCELLED);

        if (existingBooking.isPresent()) {
            throw new BookingAlreadyPresentException(existingBooking.get().getQuantity());
        }

        int result = eventRepository.decreaseCapacity(eventId, quantity);

        if(result == 0){
            throw new NotEnoughSeatsException();
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
     * - validates that the booking exists
     * - enforces ownership or admin access
     * - enforces venue isolation
     * - checks if additional seats are available when increasing quantity
     * - updates event capacity accordingly
     * - persists the updated booking
     *
     * @param bookingId identifier of the booking
     * @param user authenticated user performing the request
     * @param userVenue venue of the requesting user
     * @param quantity new desired quantity
     * @param isAdmin whether the user has admin privileges
     * @return the updated booking as a {@link BookingDto}
     * @throws ResourceNotFoundException if the booking does not exist or does not belong to the venue
     * @throws AccessDeniedException if the user is not allowed to modify the booking
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
            throw new NotEnoughSeatsException();
        }

        booking.setQuantity(quantity);

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toDto(saved);
    }

    /**
     * Cancels an existing booking.
     *
     * Behavior:
     * - validates that the booking exists within the venue
     * - enforces ownership or admin access
     * - prevents duplicate cancellation
     * - restores event capacity
     * - marks the booking as canceled
     *
     * @param bookingId identifier of the booking
     * @param currentUser authenticated user performing the request
     * @param venue venue of the requesting user
     * @param isAdmin whether the user has admin privileges
     * @throws ResourceNotFoundException if the booking does not exist within the venue
     * @throws AccessDeniedException if the user is not allowed to cancel the booking
     * @throws BookingAlreadyCancelledException if the booking is already canceled
     */
    @Transactional
    public void cancelBooking(UUID bookingId, User currentUser, Venue venue, boolean isAdmin) {

        Booking booking = bookingRepository.findByIdAndVenue(bookingId, venue)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if(booking.getStatus().equals(CANCELLED)){
            throw new IllegalStateException("booking is already canceled");
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
     * - non-admin users can only access their own bookings
     * - admin users can access bookings of any user within the venue
     * - validates that bookings exist for the user within the venue
     * - maps results to DTOs
     *
     * @param userId identifier of the target user
     * @param currentUserId identifier of the requesting user
     * @param venue venue of the requesting user
     * @param isAdmin whether the user has admin privileges
     * @return list of bookings as {@link BookingDto}
     * @throws ResourceNotFoundException if no bookings are found for the user within the venue
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