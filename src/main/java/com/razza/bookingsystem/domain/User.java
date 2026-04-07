package com.razza.bookingsystem.domain;

import lombok.*;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Represents a user in the booking system.
 * Users belong to a tenant and can have two roles (ADMIN, USER).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "app_user",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"email", "tenant_id"})},
        indexes = {@Index(name = "user_index", columnList = "tenant_id")}
)
public class User {

    /** Primary key for the user. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Email address of the user. Must be unique across the system. */
    private String email;

    /** Hashed password of the user. */
    private String password;

    /** Role of the user (defines access level). */
    @Enumerated(EnumType.STRING)
    private Role role;

    /** Tenant the user belongs to */
    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
}