package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.PerformanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDto {

    @NotBlank
    private String name;

    private String description;

    @Positive
    private int duration;

    @NotNull
    private PerformanceType performanceType;
}