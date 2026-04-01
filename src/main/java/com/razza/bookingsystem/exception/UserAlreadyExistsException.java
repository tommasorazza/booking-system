package com.razza.bookingsystem.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email){
        super("user with this email: " + email + " already exists");
    }
}
