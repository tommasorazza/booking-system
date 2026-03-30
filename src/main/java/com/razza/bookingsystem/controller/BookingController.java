package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.BookingDto;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
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
    private final EventRepository eventRepository;

    /**
     * Creates a new booking for a given event and user.
     *
     * @param eventId the UUID of the event
     * @param userId the UUID of the user
     * @param quantity number of tickets to book
     * @return the created BookingDto
     */

    @PostMapping("/events/{eventId}/book")
    @PreAuthorize("hasRole('USER')")
    public BookingDto createBooking(@PathVariable UUID eventId,
                                    @RequestParam int quantity,
                                    Authentication authentication) {

        CustomUserDetails user = getUser(authentication);

        return bookingService.createBooking(
                eventId,
                user.getId(),
                user.getTenant(),
                quantity
        );
    }

    /**
     * Cancels a booking by its ID.
     *
     * @param id the booking UUID
     */

    @DeleteMapping("/bookings/{id}")
    @PreAuthorize("hasRole('USER')")
    public void cancelBooking(@PathVariable UUID id, Authentication authentication) {
        CustomUserDetails user = getUser(authentication);

        bookingService.cancelBooking(
                id,
                user.getId(),
                user.getTenant(),
                isAdmin(authentication)
        );
    }

    /**
     * Retrieves all bookings for a given user.
     *
     * @param userId the UUID of the user
     * @return list of BookingDto
     */

    @GetMapping("/users/{userId}/bookings")
    @PreAuthorize("hasRole('USER')")
    public List<BookingDto> getUserBookings(@PathVariable UUID userId, Authentication authentication) {
        CustomUserDetails user = getUser(authentication);

        return bookingService.getUserBookings(
                userId,
                user.getId(),
                user.getTenant(),
                isAdmin(authentication)
        );
    }

    /**
     * Returns basic information about the currently authenticated user.
     *
     * This endpoint is useful for debugging authentication and authorization.
     * It returns the username along with the granted authorities (roles).
     *
     * @param auth the Authentication object injected by Spring Security
     * @return a string containing the username and roles
     */
    @GetMapping("/whoami")
    public String whoami(Authentication auth) {
        return auth.getName() + " " + auth.getAuthorities();
    }

    /**
     * Extracts the CustomUserDetails object from the Authentication.
     *
     * This method assumes that the principal stored in the Authentication
     * is an instance of CustomUserDetails.
     *
     * @param auth the Authentication object
     * @return the authenticated user as CustomUserDetails
     * @throws ClassCastException if the principal is not of type CustomUserDetails
     */
    private CustomUserDetails getUser(Authentication auth) {
        return (CustomUserDetails) auth.getPrincipal();
    }

    /**
     * Checks whether the authenticated user has admin privileges.
     *
     * This method verifies if the user has the ROLE_ADMIN authority.
     *
     * @param auth the Authentication object
     * @return true if the user has ROLE_ADMIN, false otherwise
     */
    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}