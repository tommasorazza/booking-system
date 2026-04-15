package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Booking;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.BookingDto;
import com.razza.bookingsystem.exception.*;
import com.razza.bookingsystem.mapper.BookingMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
 * tenant isolation, and access control.
 */
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final BookingMapper bookingMapper;

    /**
     * Creates a new booking for a given event.
     *
     * Behavior:
     * - validates that the event exists
     * - enforces tenant isolation
     * - checks seat availability
     * - allows admins to book on behalf of another user within the same tenant
     * - prevents duplicate bookings for the same event and user
     * - decreases available event capacity
     * - persists the booking
     *
     * @param eventId identifier of the event
     * @param user target user for the booking (used when admin books on behalf of someone)
     * @param currentUser authenticated user performing the request
     * @param tenant tenant of the requesting user
     * @param quantity number of seats requested
     * @param isAdmin whether the current user has admin privileges
     * @return the created booking as a {@link BookingDto}
     * @throws ResourceNotFoundException if the event or user does not exist or does not belong to the tenant
     * @throws NotEnoughSeatsException if there is insufficient capacity
     * @throws BookingAlreadyPresentException if a booking already exists for the same user and event
     */
    @Transactional
    public BookingDto createBooking(UUID eventId, User user, User currentUser, Tenant tenant, int quantity, Boolean isAdmin) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        if (!event.getTenant().getId().equals(tenant.getId())) {
            throw new ResourceNotFoundException("event", eventId);
        }

        if(quantity < 1){
            throw new QuantityException(quantity);
        }

        if (isAdmin) {
            currentUser = user;

            if (!user.getTenant().getId().equals(tenant.getId())) {
                throw new ResourceNotFoundException("user");
            }
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
                .id(UUID.randomUUID())
                .user(currentUser)
                .event(event)
                .tenant(event.getTenant())
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
     * - enforces tenant isolation
     * - checks if additional seats are available when increasing quantity
     * - updates event capacity accordingly
     * - persists the updated booking
     *
     * @param bookingId identifier of the booking
     * @param user authenticated user performing the request
     * @param userTenant tenant of the requesting user
     * @param quantity new desired quantity
     * @param isAdmin whether the user has admin privileges
     * @return the updated booking as a {@link BookingDto}
     * @throws ResourceNotFoundException if the booking does not exist or does not belong to the tenant
     * @throws AccessDeniedException if the user is not allowed to modify the booking
     * @throws NotEnoughSeatsException if there is insufficient capacity for the requested increase
     */
    @Transactional
    public BookingDto modifyQuantity(UUID bookingId, User user, Tenant userTenant, int quantity, Boolean isAdmin) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        Event event = booking.getEvent();

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        if (!isAdmin && !user.equals(booking.getUser())) {
            throw new AccessDeniedException("Not allowed to modify this booking");
        }

        if (!booking.getEvent().getTenant().getId().equals(userTenant.getId())) {
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
     * - validates that the booking exists within the tenant
     * - enforces ownership or admin access
     * - prevents duplicate cancellation
     * - restores event capacity
     * - marks the booking as canceled
     *
     * @param bookingId identifier of the booking
     * @param currentUser authenticated user performing the request
     * @param tenant tenant of the requesting user
     * @param isAdmin whether the user has admin privileges
     * @throws ResourceNotFoundException if the booking does not exist within the tenant
     * @throws AccessDeniedException if the user is not allowed to cancel the booking
     * @throws BookingAlreadyCancelledException if the booking is already canceled
     */
    public void cancelBooking(UUID bookingId, User currentUser, Tenant tenant, boolean isAdmin) {

        Booking booking = bookingRepository.findByIdAndTenant(bookingId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        Event event = booking.getEvent();

        if(event.getDate().isBefore(OffsetDateTime.now())){
            throw new PastEventException(event.getDate());
        }

        if (!isAdmin && !booking.getUser().equals(currentUser)) {
            throw new AccessDeniedException("Access denied");
        }

        if (!booking.getTenant().getId().equals(tenant.getId())) {
            throw new ResourceNotFoundException("booking", bookingId);
        }

        if (booking.getStatus() == CANCELLED) {
            throw new BookingAlreadyCancelledException();
        }

        event.setAvailableCapacity(event.getAvailableCapacity() + booking.getQuantity());
        eventRepository.save(event);

        booking.setStatus(CANCELLED);
        bookingRepository.save(booking);
    }

    /**
     * Retrieves all bookings for a specific user within a tenant.
     *
     * Behavior:
     * - non-admin users can only access their own bookings
     * - admin users can access bookings of any user within the tenant
     * - validates that bookings exist for the user within the tenant
     * - maps results to DTOs
     *
     * @param userId identifier of the target user
     * @param currentUserId identifier of the requesting user
     * @param tenant tenant of the requesting user
     * @param isAdmin whether the user has admin privileges
     * @return list of bookings as {@link BookingDto}
     * @throws ResourceNotFoundException if no bookings are found for the user within the tenant
     */
    public List<BookingDto> getUserBookings(UUID userId, UUID currentUserId, Tenant tenant, boolean isAdmin) {

        if (!isAdmin){
            userId = currentUserId;
        }

        if (bookingRepository.findByUserIdAndTenant(userId, tenant).isEmpty()) {
            throw new ResourceNotFoundException("user", userId);
        }

        return bookingRepository.findByUserIdAndTenant(userId, tenant)
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }
}