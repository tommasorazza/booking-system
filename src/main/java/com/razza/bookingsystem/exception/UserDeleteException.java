package com.razza.bookingsystem.exception;

/**
 * Thrown when an attempt is made to delete an user that has
 * existing confirmed bookings.
 *
 * This exception enforces the business rule that a user
 * cannot be deleted if there are existing bookings.
 */
public class UserDeleteException extends RuntimeException {

    /**
     * Constructs a new UserDeleteException with a message indicating
     * the number of active bookings preventing the event deletion.
     *
     * @param activeBookings the number of bookings confirmed for the user
     */
    public UserDeleteException(int activeBookings) {
        super("Can't delete user since " + activeBookings + " bookings already present");
    }
}