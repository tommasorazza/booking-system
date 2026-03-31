package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Booking;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.BookingDto;
import com.razza.bookingsystem.mapper.BookingMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/**
 * Service responsible for managing bookings.
 *
 * Handles:
 * - booking creation
 * - booking modification
 * - booking cancellation
 * - capacity updates
 *
 * Ensures business rules such as seat availability,
 * tenant isolation, and access control.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    /**
     * Repository for booking persistence.
     */
    private final BookingRepository bookingRepository;

    /**
     * Repository for event persistence.
     */
    private final EventRepository eventRepository;

    /**
     * Mapper for converting Booking entities to DTOs.
     */
    private final BookingMapper bookingMapper;

    /**
     * Repository for user persistence.
     */
    private final UserRepository userRepository;

    /**
     * Creates a new booking for a given event.
     *
     * Behavior:
     * - validates event existence
     * - enforces tenant isolation
     * - checks seat availability
     * - allows admins to book on behalf of another user
     * - prevents duplicate bookings for the same event
     * - reduces available capacity
     * - persists the booking
     *
     * @param eventId identifier of the event
     * @param userId target user identifier when admin is booking
     * @param currentUserId identifier of the authenticated user
     * @param tenant tenant context of the request
     * @param quantity number of seats requested
     * @param isAdmin whether the current user has admin privileges
     * @return created booking as a DTO
     * @throws RuntimeException if event not found, tenant mismatch,
     *                          insufficient capacity, duplicate booking,
     *                          or invalid user context
     */
    @Transactional
    public BookingDto createBooking(UUID eventId, UUID userId, UUID currentUserId, Tenant tenant, int quantity, Boolean isAdmin) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Cross-tenant booking not allowed");
        }

        if (event.getAvailableCapacity() < quantity) {
            throw new RuntimeException("Not enough seats available");
        }

        if (isAdmin) {
            currentUserId = userId;
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("user not found"));

            if (!user.getTenant().getId().equals(tenant.getId())) {
                throw new RuntimeException("the user is unknown within this tenant");
            }
        }

        if (bookingRepository.findByUserIdAndEvent(currentUserId, event).isPresent()) {
            throw new RuntimeException("User already has a booking for this event");
        }

        event.setAvailableCapacity(event.getAvailableCapacity() - quantity);
        eventRepository.save(event);

        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .userId(currentUserId)
                .event(event)
                .tenant(event.getTenant())
                .quantity(quantity)
                .status(com.razza.bookingsystem.domain.Status.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toDto(saved);
    }

    /**
     * Modifies the quantity of an existing booking.
     *
     * Behavior:
     * - validates booking existence
     * - enforces ownership or admin access
     * - enforces tenant isolation
     * - checks additional capacity requirements
     * - updates event capacity accordingly
     * - persists updated booking
     *
     * @param bookingId identifier of the booking
     * @param userId identifier of the requesting user
     * @param userTenant tenant of the requesting user
     * @param quantity new desired quantity
     * @param isAdmin whether the user has admin privileges
     * @return updated booking as a DTO
     * @throws RuntimeException if booking not found, access denied,
     *                          tenant mismatch, or insufficient capacity
     */
    @Transactional
    public BookingDto modifyQuantity(UUID bookingId, UUID userId, Tenant userTenant, int quantity, Boolean isAdmin) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!isAdmin && !userId.equals(booking.getUserId())) {
            throw new RuntimeException("Not allowed to modify this booking");
        }

        if (!booking.getEvent().getTenant().getId().equals(userTenant.getId())) {
            throw new RuntimeException("Cross-tenant booking modification not allowed");
        }

        if (booking.getEvent().getAvailableCapacity() < quantity - booking.getQuantity()) {
            throw new RuntimeException("Not enough seats available");
        }

        booking.getEvent().setAvailableCapacity(
                booking.getEvent().getAvailableCapacity() - (quantity - booking.getQuantity())
        );

        eventRepository.save(booking.getEvent());

        booking.setQuantity(quantity);

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toDto(saved);
    }

    /**
     * Cancels an existing booking.
     *
     * Behavior:
     * - validates booking existence within tenant
     * - enforces ownership or admin access
     * - prevents duplicate cancellation
     * - restores event capacity
     * - marks booking as cancelled
     *
     * @param bookingId identifier of the booking
     * @param currentUserId identifier of the requesting user
     * @param tenant tenant context
     * @param isAdmin whether the user has admin privileges
     * @throws RuntimeException if booking not found, already cancelled,
     *                          or tenant mismatch
     * @throws EntityNotFoundException if access is denied
     */
    @Transactional
    public void cancelBooking(UUID bookingId, UUID currentUserId, Tenant tenant, boolean isAdmin) {

        Booking booking = bookingRepository.findByIdAndTenant(bookingId, tenant)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!isAdmin && !booking.getUserId().equals(currentUserId)) {
            throw new EntityNotFoundException("Access denied");
        }

        if (!booking.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Cross-tenant access denied");
        }

        if (booking.getStatus() == com.razza.bookingsystem.domain.Status.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        Event event = booking.getEvent();

        event.setAvailableCapacity(event.getAvailableCapacity() + booking.getQuantity());
        eventRepository.save(event);

        booking.setStatus(com.razza.bookingsystem.domain.Status.CANCELLED);
        bookingRepository.save(booking);
    }

    /**
     * Retrieves all bookings for a specific user within a tenant.
     *
     * Behavior:
     * - enforces that users can only access their own bookings unless admin
     * - validates that the user has bookings in the tenant
     * - maps results to DTOs
     *
     * @param userId identifier of the target user
     * @param currentUserId identifier of the requesting user
     * @param tenant tenant context
     * @param isAdmin whether the user has admin privileges
     * @return list of bookings as DTOs
     * @throws AccessDeniedException if access is not allowed
     * @throws RuntimeException if user has no bookings in the tenant
     */
    public List<BookingDto> getUserBookings(UUID userId, UUID currentUserId, Tenant tenant, boolean isAdmin) {

        if (!isAdmin && !userId.equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (bookingRepository.findByUserIdAndTenant(userId, tenant).isEmpty()) {
            throw new RuntimeException("user not found");
        }

        return bookingRepository.findByUserIdAndTenant(userId, tenant)
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }
}