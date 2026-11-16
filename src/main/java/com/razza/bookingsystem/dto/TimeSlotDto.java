package com.razza.bookingsystem.dto;

import lombok.*;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object representing one slot in the schedule of an event.
 * Used to return which performer plays and when, without exposing the performance details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotDto {

    /** Email of the performer playing in this slot. */
    private String userEmail;

    /** Date and time when the slot starts. */
    private OffsetDateTime startTime;

    /** Date and time when the slot ends. */
    private OffsetDateTime endTime;
}