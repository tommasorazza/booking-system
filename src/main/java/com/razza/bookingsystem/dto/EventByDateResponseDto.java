package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.TimeSlot;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing an event.
 * Used to return event details as a response.
 */
@Getter
@Setter
public class EventByDateResponseDto {

    private String name;

    private String description;

    private String location;

    private OffsetDateTime date;

    private List<TimeSlotDto> schedule = new ArrayList<>();

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