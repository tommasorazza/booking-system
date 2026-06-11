package com.razza.bookingsystem.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Represents a performance belonging to one performer,
 * contains details about it.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "performance",
        indexes = {@Index(name = "performance_index", columnList = "user_id")}
)
public class Performance {

    /** Primary key for the performance. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.EAGER)
    private User user;

    /** name of the performance */
    private String name;

    /** description of the performance */
    private String description;

    /** duration in minutes of the performance */
    private int duration;

    /** type of performance */
    @Enumerated(EnumType.STRING)
    private PerformanceType performanceType;
}