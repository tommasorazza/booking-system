package com.razza.bookingsystem.exception;

public class EmptyScheduleException extends RuntimeException {
    public EmptyScheduleException(){
        super("The schedule can't be empty");
    }
}
