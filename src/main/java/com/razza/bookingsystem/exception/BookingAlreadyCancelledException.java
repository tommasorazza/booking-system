package com.razza.bookingsystem.exception;

public class BookingAlreadyCancelledException extends RuntimeException {
    public BookingAlreadyCancelledException(){

        super("booking already cancelled");
    }
}
