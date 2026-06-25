package com.razza.bookingsystem.exception;

import com.razza.bookingsystem.domain.TimeSlot;

public class InvalidScheduleException extends RuntimeException {

    private final Boolean conflict;

    public InvalidScheduleException(String string, Boolean conflict) {
        super("Schedule problem : " + string);
        this.conflict = conflict;
    }

    public Boolean isConflict() {
        return conflict;
    }

    public static InvalidScheduleException overlapping() {
        return new InvalidScheduleException("the event can't be created or updated as such because it is overlapping with an other event", true);
    }

    public static InvalidScheduleException unregisteredEmail(String email) {
        return new InvalidScheduleException(email + " is not registered", false);
    }

    public static InvalidScheduleException slotInThePast() {
        return new InvalidScheduleException("slots start before the start of the event", false);
    }

    public static InvalidScheduleException inconsistentSlots() {
        return new InvalidScheduleException("some slots overlap", true);
    }

    public static InvalidScheduleException unavailablePerformer(String email) {
        return new InvalidScheduleException("user " + email + " is not available on this date", true);
    }

    public static InvalidScheduleException alreadyBusyPerformer(String email) {
        return new InvalidScheduleException("user " + email + " is already scheduled in an other location on this date", true);
    }

    public static InvalidScheduleException inconsistentSlots(TimeSlot slot){
        return new InvalidScheduleException("timeslot starting at " + slot.getStartTime() + " does not have the same duration of its performance", false);
    }

    public static InvalidScheduleException latePerformance(){
        return new InvalidScheduleException("The starting time of the first performance is more than 10 hours after the starting of the event", false);
    }
}
