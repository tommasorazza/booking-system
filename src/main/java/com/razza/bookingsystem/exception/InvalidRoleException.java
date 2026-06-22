package com.razza.bookingsystem.exception;

public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException(String string){
        super("Wrong role: " + string);
    }

    public static InvalidRoleException wrongSignUp() {
        return new InvalidRoleException("it must be either GUEST or PERFORMER");
    }

    public static InvalidRoleException wrongBookingRole() {
        return new InvalidRoleException("you can't create a booking for a performer");
    }
}
