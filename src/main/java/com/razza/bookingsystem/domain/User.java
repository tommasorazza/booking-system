package com.razza.bookingsystem.domain;

import com.razza.bookingsystem.dto.AvailabilityDto;
import jakarta.annotation.Nullable;
import lombok.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a user in the booking system.
 * Users belong to a venue and can have two roles (ADMIN, GUEST).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "app_user",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"email", "venue_id"})},
        indexes = {@Index(name = "user_index", columnList = "venue_id")}
)
public class User {

    /** Primary key for the user. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private OffsetDateTime birthDate;

    /** Email address of the user. Must be unique across the system. */
    private String email;

    /** Hashed password of the user. */
    private String password;

    /** Role of the user (defines access level). */
    @Enumerated(EnumType.STRING)
    private Role role;

    /** Venue the user belongs to */
    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Nullable
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Performance> performances;

    /** For performers, it indicates the weekly availability */
    @Nullable
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)        // CascadeType.ALL is used so that when I save user into userRepository, I automatically save the user.availability into availabilityRepository
    private Availability availability;
}