package com.razza.bookingsystem.controller;

import com.razza.bookingsystem.dto.BookingDto;
import com.razza.bookingsystem.service.BookingService;
import lombok.RequiredArgsConstructor;
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

    /**
     * Creates a new booking for a given event and user.
     *
     * @param eventId the UUID of the event
     * @param userId the UUID of the user
     * @param quantity number of tickets to book
     * @return the created BookingDto
     */
    @PostMapping("/events/{eventId}/book")
    public BookingDto createBooking(@PathVariable UUID eventId,
                                    @RequestParam UUID userId,
                                    @RequestParam int quantity) {
        return bookingService.createBooking(eventId, userId, quantity);
    }

    /**
     * Cancels a booking by its ID.
     *
     * @param id the booking UUID
     */
    @DeleteMapping("/bookings/{id}")
    public void cancelBooking(@PathVariable UUID id) {
        bookingService.cancelBooking(id);
    }

    /**
     * Retrieves all bookings for a given user.
     *
     * @param userId the UUID of the user
     * @return list of BookingDto
     */
    @GetMapping("/users/{userId}/bookings")
    public List<BookingDto> getUserBookings(@PathVariable UUID userId) {
        return bookingService.getUserBookings(userId);
    }
}