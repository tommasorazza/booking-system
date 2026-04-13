package com.razza.bookingsystem.exception;

import java.time.OffsetDateTime;

/**
 * Exception thrown when an operation is attempted on an event
 * that is scheduled in the past.
 *
 * This is typically used to prevent creating or updating bookings
 * for events whose date has already passed.
 */
public class PastEventException extends RuntimeException {

    /**
     * Constructs a new PastEventException with the specified event date.
     *
     * @param eventDate the date of the event that is in the past
     */
    public PastEventException(OffsetDateTime eventDate) {
        super("error: event in the past: " + eventDate);
    }
}