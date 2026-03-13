package com.razza.bookingsystem.dto;

import lombok.*;
import java.util.UUID;

/**
 * Data Transfer Object representing a tenant.
 * Used to transfer tenant details between layers.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDto {

    /** Unique identifier for the tenant. */
    private UUID id;

    /** Name of the tenant (e.g., company or organization). */
    private String name;
}