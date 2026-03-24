package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.BookingDto;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for booking operations.
 * Supports booking creation, cancellation, and retrieval.
 */
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    /**
     * Creates a new booking for a given event and user.
     *
     * @param eventId the UUID of the event
     * @param userId the UUID of the user
     * @param quantity number of tickets to book
     * @return the created BookingDto
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/events/{eventId}/book")
    public BookingDto createBooking(@PathVariable UUID eventId,
                                    @RequestParam int quantity,
                                    Authentication authentication) {
        return bookingService.createBooking(eventId,getUserId(authentication), quantity);
    }

    /**
     * Cancels a booking by its ID.
     *
     * @param id the booking UUID
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/bookings/{id}")
    public void cancelBooking(@PathVariable UUID id, Authentication authentication) {
        bookingService.cancelBooking(id, getUserId(authentication), isAdmin(authentication));
    }

    /**
     * Retrieves all bookings for a given user.
     *
     * @param userId the UUID of the user
     * @return list of BookingDto
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/users/{userId}/bookings")
    public List<BookingDto> getUserBookings(@PathVariable UUID userId, Authentication authentication) {
        return bookingService.getUserBookings(userId, getUserId(authentication), isAdmin(authentication));
    }
    private UUID getUserId(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getId();
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}