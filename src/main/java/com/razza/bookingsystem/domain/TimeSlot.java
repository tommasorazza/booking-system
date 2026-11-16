package com.razza.bookingsystem.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Embeddable representing one slot in the schedule of an event.
 * Each slot tells which performer plays, which performance they play, and when.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class TimeSlot {

    /** Email of the performer playing in this slot. */
    private String userEmail;

    /** Date and time when the slot starts. */
    private OffsetDateTime startTime;

    /** Date and time when the slot ends. */
    private OffsetDateTime endTime;

    /** ID of the performance played in this slot. */
    private UUID performanceId;

}