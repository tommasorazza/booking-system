package com.razza.bookingsystem.exception;

public class BadCapacityException extends RuntimeException {
    public BadCapacityException() {
        super("The total capacity should be between 0 and 10000, also available capacity (if present) should be less or equal than total capacity");
    }
}
