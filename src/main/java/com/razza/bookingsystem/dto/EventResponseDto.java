package com.razza.bookingsystem.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing an event.
 * Used to return event details as a response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDto {

    /** Unique identifier for the event. */
    private UUID id;

    /** Name/title of the event. */
    private String name;

    /** Description or details about the event. */
    private String description;

    /** Location where the event will take place. */
    private String location;

    /** Date and time of the event. */
    private OffsetDateTime date;

    /** Total number of seats available for the event. */
    private int totalCapacity;

    /** Number of seats still available for booking. */
    private int availableCapacity;
}