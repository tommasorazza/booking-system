package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.PerformanceType;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledPerformanceDto {

    private String name;

    private String description;

    private int duration;

    private PerformanceType performanceType;

    private OffsetDateTime startTime;

    private String location;
}
