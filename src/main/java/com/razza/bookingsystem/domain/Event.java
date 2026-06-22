package com.razza.bookingsystem.domain;

import io.micrometer.common.lang.Nullable;
import lombok.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a event that users can book tickets for.
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
        indexes = {@Index(name = "event_user_index", columnList = "venueId")}
)
public class Event {

    /** Primary key for the event. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Name of the event. */
    private String name;

    /** Description or details about the event. */
    private String description;

    /** Location where the event will take place. */
    private String location;

    /** Date and time of the event. */
    private OffsetDateTime date;

    @Nullable
    @Embedded
    private BookingPolicy bookingPolicy;

    /** Version field for optimistic locking. */
    @Version
    private Long version;

    /** Venue the event belongs to */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    /** Status of the event (CONFIRMED/CANCELLED) */
    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection
    @CollectionTable(name = "scheduleTable", joinColumns = @JoinColumn(name = "event_id"))
    private List<TimeSlot> schedule;

    private Boolean eighteenPlus;

    public OffsetDateTime getEndTime() {
        return schedule.getLast().getEndTime();
    }

    }