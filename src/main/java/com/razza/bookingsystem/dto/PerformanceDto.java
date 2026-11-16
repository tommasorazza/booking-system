package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.PerformanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * Data Transfer Object representing a performance.
 * Used for creating performances.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDto {

    /** Name of the performance. */
    @NotBlank
    private String name;

    /** Description of the performance. */
    private String description;

    /** Duration in minutes of the performance. */
    @Positive
    private int duration;

    /** Type of performance. */
    @NotNull
    private PerformanceType performanceType;
}