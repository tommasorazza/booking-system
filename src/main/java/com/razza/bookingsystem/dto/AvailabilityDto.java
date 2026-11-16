package com.razza.bookingsystem.dto;

import lombok.*;

/**
 * Data transfer object for the weekly availability of a performer.
 * Each day of the week is a Boolean flag telling whether the performer is available on that day.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvailabilityDto {

    private Boolean monday;
    private Boolean tuesday;
    private Boolean wednesday;
    private Boolean thursday;
    private Boolean friday;
    private Boolean saturday;
    private Boolean sunday;
}