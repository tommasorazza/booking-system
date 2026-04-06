package com.razza.bookingsystem.exception;

public class BookingAlreadyPresentException extends RuntimeException {
    public BookingAlreadyPresentException(int quantity){
        super("user already has a booking of " + quantity + " seats for this event");
    }
}
