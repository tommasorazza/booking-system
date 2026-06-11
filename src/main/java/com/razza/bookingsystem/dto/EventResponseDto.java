package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.TimeSlot;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a event.
 * Used to return event details as a response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDto {

    /** Name/title of the event. */
    private String name;

    /** Description or details about the event. */
    private String description;

    /** Location where the event will take place. */
    private String location;

    /** Date and time of the event. */
    private OffsetDateTime date;

    /** Total number of seats available for the event. */
    private Integer totalCapacity;

    /** Number of seats still available for booking. */
    private Integer availableCapacity;

    private List<TimeSlot> schedule;

    private Boolean eighteenPlus;
}