package com.razza.bookingsystem.exception;

/**
 * Thrown when an attempt is made to register a user with an email
 * that is already associated with an existing account.
 *
 * This exception enforces the business rule that each user must have a unique email.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new EmailAlreadyExistsException with a message
     * indicating the email that is already in use.
     *
     * @param email the email address that is already registered
     */
    public EmailAlreadyExistsException(String email) {
        super("Email already in use: " + email);
    }
}