package com.razza.bookingsystem.exception;

/**
 * Thrown when a user tries to book an event restricted to users aged 18 or older
 * and they are younger than that.
 *
 * This exception is a subclass of RuntimeException and indicates a business rule violation.
 */
public class invalidAgeException extends RuntimeException {

    /**
     * Constructs a new invalidAgeException with a default message.
     */
    public invalidAgeException(){
        super("You must be 18+ for this event");
    }
}