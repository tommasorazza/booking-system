package com.razza.bookingsystem.exception;

public class MissingAvailabilityException extends RuntimeException {
    public MissingAvailabilityException(){
        super("Availability must be specified since you're a performer");
    }
}
