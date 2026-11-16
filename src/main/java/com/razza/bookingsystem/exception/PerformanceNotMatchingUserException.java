package com.razza.bookingsystem.exception;

import java.util.UUID;

/**
 * Thrown when a schedule includes a performance that does not belong to the performer it is assigned to.
 *
 * This exception is a subclass of RuntimeException and indicates a business rule violation.
 */
public class PerformanceNotMatchingUserException extends RuntimeException {

    /**
     * Constructs a new PerformanceNotMatchingUserException with a message naming the performance and the performer.
     *
     * @param performanceId the ID of the performance
     * @param performerEmail the email of the performer it was assigned to
     */
    public PerformanceNotMatchingUserException(UUID performanceId, String performerEmail){
        super(performanceId + " does not belong to " + performerEmail);
    }
}