package com.razza.bookingsystem.exception;

/**
 * Thrown when an event is created or updated with an empty schedule.
 *
 * This exception is a subclass of RuntimeException and indicates a business rule violation.
 */
public class EmptyScheduleException extends RuntimeException {

    /**
     * Constructs a new EmptyScheduleException with a default message.
     */
    public EmptyScheduleException(){
        super("The schedule can't be empty");
    }
}