package com.razza.bookingsystem.exception;

public class EventDeleteException extends RuntimeException {

    public EventDeleteException(int activeBookings) {
        super("can't delete event since " + activeBookings + " bookings already present");
    }
}