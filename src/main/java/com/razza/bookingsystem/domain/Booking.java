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
        uniqueConstraints = {@UniqueConstraint(columnNames = {"userId", "event_id"})},
        indexes = { @Index(name = "booking_user_index", columnList = "userId"),
                @Index(name = "booking_event_index", columnList = "event_id"),
                @Index(name = "booking_tenant_index", columnList = "tenantId")}
)
public class Booking {

    /** Primary key for the booking. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID of the user who made the booking. */
    private UUID userId;

    /** event that is being booked. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    /** Number of tickets booked. */
    private int quantity;

    /** Current status of the booking (CONFIRMED, CANCELLED). */
    @Enumerated(EnumType.STRING)
    private Status status;

    /** Timestamp when the booking was created. */
    private LocalDateTime createdAt;
}