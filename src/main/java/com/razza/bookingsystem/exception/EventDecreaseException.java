package com.razza.bookingsystem.exception;

/**
 * Thrown when an attempt is made to decrease the capacity of an event
 * below the number of already confirmed bookings.
 *
 * This exception enforces the business rule that an event's capacity
 * cannot be reduced if there are existing bookings.
 */
public class EventDecreaseException extends RuntimeException {

    /**
     * Constructs a new EventDecreaseException with a message indicating
     * the number of active bookings preventing the capacity decrease.
     *
     * @param activeBookings the number of bookings already confirmed for the event
     */
    public EventDecreaseException(int activeBookings) {
        super("Can't decrease event capacity since " + activeBookings + " bookings already present");
    }
}