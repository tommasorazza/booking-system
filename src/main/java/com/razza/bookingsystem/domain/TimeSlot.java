package com.razza.bookingsystem.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class TimeSlot {

    private String userEmail;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    private UUID performanceId;

}
