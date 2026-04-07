package com.razza.bookingsystem.exception;

/**
 * Thrown when an attempt is made to create a user that already exists
 * in the system with the same email.
 *
 * This exception enforces the business rule that each user must have
 * a unique email address.
 */
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new UserAlreadyExistsException with a message indicating
     * the email that is already registered.
     *
     * @param email the email address that is already associated with an existing user
     */
    public UserAlreadyExistsException(String email) {
        super("User with this email: " + email + " already exists");
    }
}