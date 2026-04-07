package com.razza.bookingsystem.exception;

/**
 * Thrown when a user tries to book more seats than are currently available
 * for an event.
 *
 * This exception enforces the business rule that bookings cannot exceed
 * the event's available capacity.
 */
public class NotEnoughSeatsException extends RuntimeException {

    /**
     * Constructs a new NotEnoughSeatsException with a message indicating
     * the number of remaining seats available.
     *
     * @param availableSeats the number of seats still available for booking
     */
    public NotEnoughSeatsException(int availableSeats) {
        super("Not enough seats available for the booking, remaining available: " + availableSeats);
    }
}