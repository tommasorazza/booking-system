package com.razza.bookingsystem.dto;

import com.razza.bookingsystem.domain.Status;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a booking.
 * Used to transfer booking data between layers without exposing the entity directly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDto {

    /** Unique identifier for the booking. */
    private UUID id;

    /** unique identifier of event booked */
    private UUID eventId;

    /** event name */
    private String eventName;

    /** ID of the user who made the booking. */
    private UUID userId;

    /** Number of seats booked. */
    private int quantity;

    /** Current status of the booking (CONFIRMED, CANCELLED). */
    private Status status;

    /** Timestamp when the booking was created. */
    private OffsetDateTime createdAt;
}