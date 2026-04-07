package com.razza.bookingsystem.domain;

import lombok.*;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Represents a tenant in the booking system.
 * Tenants can manage users and events independently.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "tenant"
)
public class Tenant {

    /** Primary key for the tenant. Generated as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Name of the tenant (like a company or organization name). */
    private String name;
}