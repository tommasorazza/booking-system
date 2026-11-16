package com.razza.bookingsystem.exception;

/**
 * Exception thrown when the capacity of an event is not valid.
 * It happens if the total capacity is out of range, or if the available capacity
 * is negative or greater than the total capacity.
 */
public class BadCapacityException extends RuntimeException {

    /**
     * Creates the exception with a message explaining the allowed capacity values.
     */
    public BadCapacityException() {
        super("The total capacity should be between 0 and 10000, also available capacity (if present) should be less or equal than total capacity");
    }
}