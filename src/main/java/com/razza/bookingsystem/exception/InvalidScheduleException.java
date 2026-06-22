package com.razza.bookingsystem.exception;

import com.razza.bookingsystem.domain.TimeSlot;

public class InvalidScheduleException extends RuntimeException {
    public InvalidScheduleException(){
        super("The schedule is not valid");
    }

    public InvalidScheduleException(String string) {
        super("Schedule problem : " + string);
    }

    public static InvalidScheduleException overlapping() {
        return new InvalidScheduleException("the event can't be created or updated as such because it is overlapping with an other event");
    }

    public static InvalidScheduleException unregisteredEmail(String email) {
        return new InvalidScheduleException(email + " is not registered");
    }

    public static InvalidScheduleException slotInThePast() {
        return new InvalidScheduleException("slots start before the start of the event");
    }

    public static InvalidScheduleException inconsistentSlots() {
        return new InvalidScheduleException("some slots overlap");
    }

    public InvalidScheduleException(TimeSlot slot){
        super("timeslot starting at " + slot.getStartTime() + " does not have the same duration of its performance");
    }

    public InvalidScheduleException(long minutes){
        super("The starting time of the first performance is more than 10 hours after the starting of the event");
    }
}
