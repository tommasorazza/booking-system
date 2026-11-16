package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.TimeSlot;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing an event returned when looking up events by date.
 * Used to return event details together with the schedule as a response.
 */
@Getter
@Setter
public class EventByDateResponseDto {

    /** Name of the event. */
    private String name;

    /** Description or details about the event. */
    private String description;

    /** Location where the event takes place. */
    private String location;

    /** Date and time of the event. */
    private OffsetDateTime date;

    /** Schedule of the event, as a list of time slots. */
    private List<TimeSlotDto> schedule = new ArrayList<>();

    /**
     * Creates the response from the event details and its schedule.
     * Each time slot is converted to a TimeSlotDto.
     *
     * @param name the name of the event
     * @param description the description of the event
     * @param location the location of the event
     * @param date the date and time of the event
     * @param schedule the time slots of the event
     */
    public EventByDateResponseDto(String name, String description, String location, OffsetDateTime date, List<TimeSlot> schedule) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.date = date;
        for(TimeSlot slot : schedule){
            this.schedule.add(new TimeSlotDto(slot.getUserEmail(), slot.getStartTime(), slot.getEndTime()));
        }
    }
}