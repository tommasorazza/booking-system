package com.razza.bookingsystem.exception;

public class NotEnoughSeatsException extends RuntimeException {
    public NotEnoughSeatsException(int availableSeats){
        super("not enough seats available for the booking, remaining available: " + availableSeats);
    }
}
