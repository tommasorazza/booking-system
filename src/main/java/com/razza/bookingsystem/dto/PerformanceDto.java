package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.PerformanceType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDto {

    private String name;

    private String description;

    private int duration;

    private PerformanceType performanceType;
}
