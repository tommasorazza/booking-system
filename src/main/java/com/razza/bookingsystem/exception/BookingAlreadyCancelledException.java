package com.razza.bookingsystem.exception;

/**
 * Thrown when an attempt is made to cancel a booking that has already been canceled.
 *
 * This exception is a subclass of RuntimeException and indicates a business rule violation.
 */
public class BookingAlreadyCancelledException extends RuntimeException {

    /**
     * Constructs a new BookingAlreadyCancelledException with a default message.
     */
    public BookingAlreadyCancelledException() {
        super("booking already cancelled");
    }
}