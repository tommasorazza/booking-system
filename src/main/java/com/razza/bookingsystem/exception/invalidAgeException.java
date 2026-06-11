package com.razza.bookingsystem.exception;

public class invalidAgeException extends RuntimeException {
    public invalidAgeException(){
        super("You must be 18+ for this event");
    }
}
