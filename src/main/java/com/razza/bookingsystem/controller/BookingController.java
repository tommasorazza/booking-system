package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.BookingDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for booking operations.
 * Provides endpoints for creating, updating, cancelling, and retrieving bookings.
 */
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    /**
     * Creates a new booking for an event.
     *
     * If the authenticated user is an admin, a booking can be created on behalf of another user.
     * Otherwise, the booking is created for the authenticated user.
     *
     * @param eventId the ID of the event to book
     * @param quantity the number of tickets to book
     * @param userId optional ID of the user (used only by admins)
     * @param user the authenticated user details
     * @return the created booking as a BookingDto
     */
    @PostMapping("/bookings/{eventId}/book")
    public BookingDto createBooking(@PathVariable UUID eventId,
                                    @RequestParam int quantity,
                                    @RequestParam(required = false) UUID userId,
                                    @AuthenticationPrincipal CustomUserDetails user) {

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));

        Boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user", user.getId()));

        return bookingService.createBooking(
                eventId,
                targetUser,
                currentUser,
                user.getTenant(),
                quantity,
                isAdmin
        );
    }

    /**
     * Updates the quantity of an existing booking.
     *
     * Users can modify their own bookings. Admins can modify any booking within their tenant.
     *
     * @param bookingId the ID of the booking to update
     * @param quantity the new quantity of tickets
     * @param authentication the current authenticated user
     * @return the updated booking as a BookingDto
     */
    @PutMapping("/bookings/{bookingId}")
    public BookingDto modifyQuantity(@PathVariable UUID bookingId,
                                     @RequestParam int quantity,
                                     Authentication authentication) {

        CustomUserDetails user = getUser(authentication);

        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user", user.getId()));

        return bookingService.modifyQuantity(
                bookingId,
                currentUser,
                user.getTenant(),
                quantity,
                isAdmin(authentication)
        );
    }

    /**
     * Cancels a booking.
     *
     * Users can cancel their own bookings. Admins can cancel any booking within their tenant.
     *
     * @param id the ID of the booking to cancel
     * @param authentication the current authenticated user
     */
    @DeleteMapping("/bookings/{id}")
    public void cancelBooking(@PathVariable UUID id, Authentication authentication) {
        CustomUserDetails user = getUser(authentication);

        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user", user.getId()));

        bookingService.cancelBooking(
                id,
                currentUser,
                user.getTenant(),
                isAdmin(authentication)
        );
    }

    /**
     * Retrieves bookings for a user.
     *
     * If the authenticated user is an admin, bookings for another user can be retrieved.
     * Otherwise, only the authenticated user's bookings are returned.
     *
     * @param userId optional ID of the user whose bookings are requested
     * @param authentication the current authenticated user
     * @return list of BookingDto
     */
    @GetMapping("/bookings/userBookings")
    public List<BookingDto> getUserBookings(@RequestParam(required = false) UUID userId,
                                            Authentication authentication) {

        CustomUserDetails user = getUser(authentication);

        return bookingService.getUserBookings(
                userId,
                user.getId(),
                user.getTenant(),
                isAdmin(authentication)
        );
    }

    /**
     * Returns information about the currently authenticated user.
     *
     * Useful for debugging authentication and verifying assigned roles.
     *
     * @param auth the Authentication object
     * @return a string containing the username and roles
     */
    @GetMapping("/whoami")
    public String whoami(Authentication auth) {
        return auth.getName() + " " + auth.getAuthorities();
    }

    /**
     * Extracts the authenticated user's details.
     *
     * @param auth the Authentication object
     * @return the authenticated user details
     */
    private CustomUserDetails getUser(Authentication auth) {
        return (CustomUserDetails) auth.getPrincipal();
    }

    /**
     * Checks whether the authenticated user has admin privileges.
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