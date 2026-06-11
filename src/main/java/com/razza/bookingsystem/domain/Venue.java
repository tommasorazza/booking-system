package com.razza.bookingsystem.domain;

import lombok.*;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Represents a venue in the booking system.
 * Venues can manage users and events independently.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "venue"
)
public class Venue {

    /** Primary key for the venue. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Name of the venue (like a company or organization name). */
    private String name;
}