package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.TimeSlot;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Data Transfer Object representing a event.
 * Used for creating or updating events.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequestDto {

    /** Name/title of the event. */
    @NotBlank
    private String name;

    /** Description or details about the event. */
    private String description;

    /** Location where the event will take place. */
    @NotBlank
    private String location;

    /** Date and time of the event. */
    @NotNull
    private OffsetDateTime date;

    /** Total number of seats available for the event. */
    @Nullable
    @Positive
    private Integer totalCapacity;

    private List<TimeSlot> schedule;

    private Boolean eighteenPlus;

}