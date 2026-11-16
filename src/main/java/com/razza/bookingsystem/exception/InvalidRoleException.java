package com.razza.bookingsystem.exception;

/**
 * Thrown when a user has a role that is not allowed for the requested operation.
 *
 * This exception is a subclass of RuntimeException and indicates a business rule violation.
 */
public class InvalidRoleException extends RuntimeException {

    /**
     * Constructs a new InvalidRoleException with a message explaining the problem.
     *
     * @param string the reason why the role is not valid
     */
    public InvalidRoleException(String string){
        super("Wrong role: " + string);
    }

    /**
     * Creates the exception for a signup with a role that is not allowed.
     *
     * @return an InvalidRoleException saying the role must be GUEST or PERFORMER
     */
    public static InvalidRoleException wrongSignUp() {
        return new InvalidRoleException("it must be either GUEST or PERFORMER");
    }

    /**
     * Creates the exception for a booking made on behalf of a performer.
     *
     * @return an InvalidRoleException saying a booking can't be created for a performer
     */
    public static InvalidRoleException wrongBookingRole() {
        return new InvalidRoleException("you can't create a booking for a performer");
    }
}