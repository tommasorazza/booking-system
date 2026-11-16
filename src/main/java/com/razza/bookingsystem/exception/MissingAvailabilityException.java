package com.razza.bookingsystem.exception;

/**
 * Thrown when a performer signs up without specifying their availability.
 *
 * This exception is a subclass of RuntimeException and indicates a business rule violation.
 */
public class MissingAvailabilityException extends RuntimeException {

    /**
     * Constructs a new MissingAvailabilityException with a default message.
     */
    public MissingAvailabilityException(){
        super("Availability must be specified since you're a performer");
    }
}