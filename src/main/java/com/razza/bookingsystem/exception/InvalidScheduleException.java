package com.razza.bookingsystem.exception;

import com.razza.bookingsystem.domain.TimeSlot;

public class InvalidScheduleException extends RuntimeException {
    public InvalidScheduleException(){
        super("The schedule is not valid");
    }

    public InvalidScheduleException(String email){
        super("performer: " + email + " is not registered");
    }

    public InvalidScheduleException(TimeSlot slot){
        super("timeslot starting at" + slot.getStartTime() + " does not have the same duration of its performance");
    }
}
