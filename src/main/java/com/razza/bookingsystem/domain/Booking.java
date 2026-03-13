package com.razza.bookingsystem.domain;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a booking made by a user for a specific event.
 * A user can book multiple tickets for an event, but cannot book
 * the same event more than once (enforced by unique constraint).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "booking",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"userId", "eventId"})},
        indexes = { @Index(name = "user_index", columnList = "userId"),
                @Index(name = "event_index", columnList = "eventId"),
                @Index(name = "tenant_index", columnList = "tenantId")}
)
public class Booking {

    /** Primary key for the booking. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID of the user who made the booking. */
    private UUID userId;

    /** ID of the event that is being booked. */
    private UUID eventId;

    /** ID of the tenant the booking belongs to. */
    private UUID tenantId;

    /** Number of tickets booked. */
    private int quantity;

    /** Current status of the booking (CONFIRMED, CANCELLED). */
    @Enumerated(EnumType.STRING)
    private Status status;

    /** Timestamp when the booking was created. */
    private LocalDateTime createdAt;
}