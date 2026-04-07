package com.razza.bookingsystem.exception;

/**
 * Thrown when a user tries to create a booking for an event
 * for which they already have an existing booking.
 *
 * This exception indicates a violation of the business rule
 * that a user cannot have multiple bookings for the same event.
 */
public class BookingAlreadyPresentException extends RuntimeException {

    /**
     * Constructs a new BookingAlreadyPresentException with a message
     * indicating the quantity of seats already booked by the user.
     *
     * @param quantity the number of seats the user has already booked for the event
     */
    public BookingAlreadyPresentException(int quantity) {
        super("User already has a booking of " + quantity + " seats for this event");
    }
}