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

    /**
     * Constructs a new NotEnoughSeatsException for a booking update, where the
     * seats already held by the booking count as available again.
     * The message shows the remaining seats including those.
     *
     * @param availableSeats the number of seats still available for booking
     * @param quantity the number of seats already held by the booking
     */
    public NotEnoughSeatsException(int availableSeats, int quantity) {
        super("not enough seats available for the booking, remaining available: " + (availableSeats + quantity));
    }
}