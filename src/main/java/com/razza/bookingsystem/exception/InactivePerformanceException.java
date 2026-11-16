package com.razza.bookingsystem.exception;

import java.util.UUID;

/**
 * Thrown when an event schedule includes a performance that the performer has deactivated.
 *
 * This exception is a subclass of RuntimeException and indicates a business rule violation.
 */
public class InactivePerformanceException extends RuntimeException{

    /**
     * Constructs a new InactivePerformanceException with a message naming the performance.
     *
     * @param performanceId the ID of the deactivated performance
     */
    public InactivePerformanceException(UUID performanceId){
        super("The performance: " + performanceId + " can't be scheduled because the performer deactivated it");
    }
}