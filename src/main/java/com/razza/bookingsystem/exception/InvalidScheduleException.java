package com.razza.bookingsystem.exception;

import com.razza.bookingsystem.domain.TimeSlot;

/**
 * Thrown when the schedule of an event is not valid.
 * A conflict flag tells whether the problem is a conflict (409) or a bad request (400).
 *
 * This exception is a subclass of RuntimeException and indicates a business rule violation.
 */
public class InvalidScheduleException extends RuntimeException {

    /** Whether the problem is a conflict with other events or slots, as opposed to a bad request. */
    private final Boolean conflict;

    /**
     * Constructs a new InvalidScheduleException with a message and the conflict flag.
     *
     * @param string the reason why the schedule is not valid
     * @param conflict true if the problem is a conflict, false if it is a bad request
     */
    public InvalidScheduleException(String string, Boolean conflict) {
        super("Schedule problem : " + string);
        this.conflict = conflict;
    }

    /**
     * Tells whether the problem is a conflict.
     *
     * @return true if it is a conflict, false otherwise
     */
    public Boolean isConflict() {
        return conflict;
    }

    /**
     * Creates the exception for an event overlapping with another event.
     *
     * @return an InvalidScheduleException flagged as a conflict
     */
    public static InvalidScheduleException overlapping() {
        return new InvalidScheduleException("the event can't be created or updated as such because it is overlapping with an other event", true);
    }

    /**
     * Creates the exception for a slot with an email that is not registered.
     *
     * @param email the email that is not registered
     * @return an InvalidScheduleException not flagged as a conflict
     */
    public static InvalidScheduleException unregisteredEmail(String email) {
        return new InvalidScheduleException(email + " is not registered", false);
    }

    /**
     * Creates the exception for slots starting before the start of the event.
     *
     * @return an InvalidScheduleException not flagged as a conflict
     */
    public static InvalidScheduleException slotInThePast() {
        return new InvalidScheduleException("slots start before the start of the event", false);
    }

    /**
     * Creates the exception for slots overlapping with each other.
     *
     * @return an InvalidScheduleException flagged as a conflict
     */
    public static InvalidScheduleException inconsistentSlots() {
        return new InvalidScheduleException("some slots overlap", true);
    }

    /**
     * Creates the exception for a performer who is not available on the date of the event.
     *
     * @param email the email of the performer
     * @return an InvalidScheduleException flagged as a conflict
     */
    public static InvalidScheduleException unavailablePerformer(String email) {
        return new InvalidScheduleException("user " + email + " is not available on this date", true);
    }

    /**
     * Creates the exception for a performer already scheduled in another location on the same date.
     *
     * @param email the email of the performer
     * @return an InvalidScheduleException flagged as a conflict
     */
    public static InvalidScheduleException alreadyBusyPerformer(String email) {
        return new InvalidScheduleException("user " + email + " is already scheduled in an other location on this date", true);
    }

    /**
     * Creates the exception for a slot whose duration differs from the duration of its performance.
     *
     * @param slot the slot with the wrong duration
     * @return an InvalidScheduleException not flagged as a conflict
     */
    public static InvalidScheduleException inconsistentSlots(TimeSlot slot){
        return new InvalidScheduleException("timeslot starting at " + slot.getStartTime() + " does not have the same duration of its performance", false);
    }

    /**
     * Creates the exception for a first performance starting too long after the start of the event.
     *
     * @return an InvalidScheduleException not flagged as a conflict
     */
    public static InvalidScheduleException latePerformance(){
        return new InvalidScheduleException("The starting time of the first performance is more than 10 hours after the starting of the event", false);
    }
}