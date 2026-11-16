package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.PerformanceType;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object representing a performance together with when and where it is scheduled.
 * Used to return the performances of a performer as a response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledPerformanceDto {

    /** Name of the performance. */
    private String name;

    /** Description of the performance. */
    private String description;

    /** Duration in minutes of the performance. */
    private int duration;

    /** Type of performance. */
    private PerformanceType performanceType;

    /** Date and time when the performance starts. */
    private OffsetDateTime startTime;

    /** Location where the performance takes place. */
    private String location;
}