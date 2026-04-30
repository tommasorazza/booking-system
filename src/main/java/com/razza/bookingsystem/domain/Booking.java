package com.razza.bookingsystem.domain;

import lombok.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a booking made by a user for a specific event.
 * A user can book multiple seats for an event, but cannot book
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
        uniqueConstraints = {@UniqueConstraint(name = "booking_user_event_unique", columnNames = {"userId", "event_id"})},
        indexes = { @Index(name = "booking_user_index", columnList = "user_id"),
                @Index(name = "booking_event_index", columnList = "event_id"),
                @Index(name = "booking_tenant_index", columnList = "tenantId")}
)
public class Booking {

    /** Primary key for the booking. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** User who created the booking. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Event associated with this booking. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** Tenant to which the booking belongs. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    /** Number of seats booked. */
    private int quantity;

    /** Current status of the booking (e.g., CONFIRMED, CANCELLED). */
    @Enumerated(EnumType.STRING)
    private Status status;

    /** version field to guarantee thread safety */
    @Version
    private Long version;

    /** Timestamp when the booking was created. */
    private OffsetDateTime createdAt;
}