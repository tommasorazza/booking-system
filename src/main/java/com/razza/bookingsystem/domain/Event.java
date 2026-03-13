package com.razza.bookingsystem.domain;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an event that users can book tickets for.
 * Contains details about the event and its capacity.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "event",
        indexes = {@Index(name = "user_index", columnList = "tenantId")}
)
public class Event {

    /** Primary key for the event. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Name/title of the event. */
    private String name;

    /** Description or details about the event. */
    private String description;

    /** Location where the event will take place. */
    private String location;

    /** Date and time of the event. */
    private LocalDateTime date;

    /** Total number of tickets available for the event. */
    private int totalCapacity;

    /** Number of tickets still available for booking. */
    private int availableCapacity;

    /** Version field for optimistic locking. */
    @Version
    private Long version;

    /** ID of the tenant that owns this event. */
    private UUID tenantId;
}