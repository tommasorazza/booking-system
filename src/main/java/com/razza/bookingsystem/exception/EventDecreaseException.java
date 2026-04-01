package com.razza.bookingsystem.exception;

public class EventDecreaseException extends RuntimeException {

    public EventDecreaseException(int activeBookings) {
        super("can't decrease event capacity since " + activeBookings + " bookings already present");
    }
}