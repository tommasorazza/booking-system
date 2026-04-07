package com.razza.bookingsystem.exception;

/**
 * Thrown when an attempt is made to delete an event that has
 * existing confirmed bookings.
 *
 * This exception enforces the business rule that an event
 * cannot be deleted if there are existing bookings.
 */
public class EventDeleteException extends RuntimeException {

    /**
     * Constructs a new EventDeleteException with a message indicating
     * the number of active bookings preventing the event deletion.
     *
     * @param activeBookings the number of bookings already confirmed for the event
     */
    public EventDeleteException(int activeBookings) {
        super("Can't delete event since " + activeBookings + " bookings already present");
    }
}