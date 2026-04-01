package com.razza.bookingsystem.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("email already in use: " + email);
    }
}