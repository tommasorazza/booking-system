package com.razza.bookingsystem.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotDto {

    private String userEmail;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;
}
