package com.razza.bookingsystem.dto;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an event.
 * Used for creating or updating events.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequestDto {

    /** Name/title of the event. */
    private String name;

    /** Description or details about the event. */
    private String description;

    /** Location where the event will take place. */
    private String location;

    /** Date and time of the event. */
    private LocalDateTime date;

    /** Total number of tickets available for the event. */
    private int totalCapacity;

}