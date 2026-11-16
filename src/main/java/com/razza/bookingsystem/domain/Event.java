package com.razza.bookingsystem.domain;

import io.micrometer.common.lang.Nullable;
import lombok.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.List;
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
        indexes = {@Index(name = "event_user_index", columnList = "venue_id")}
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

    /** Capacity rules of the event. Null if the event does not require booking. */
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

    /** Schedule of the event, as a list of time slots with the performers playing in each. */
    @ElementCollection
    @CollectionTable(name = "scheduleTable", joinColumns = @JoinColumn(name = "event_id"))
    private List<TimeSlot> schedule;

    /** Whether the event is restricted to users aged 18 or older. */
    private Boolean eighteenPlus;

    /**
     * Returns the end time of the event, which is the end of the last time slot in the schedule.
     *
     * @return the end time of the last time slot
     */
    public OffsetDateTime getEndTime() {
        return schedule.getLast().getEndTime();
    }

}