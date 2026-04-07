package com.razza.bookingsystem.exception;

import java.util.UUID;

/**
 * Thrown when a requested resource (User, Event, Tenant) is not found in the system.
*/
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with a message including
     * the resource type and its UUID.
     *
     * @param resource the name of the resource
     * @param id the UUID of the resource that was not found
     */
    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + ": " + id + " not found");
    }

    /**
     * Constructs a new ResourceNotFoundException with a message including
     * only the resource name.
     *
     * @param resource the name of the resource that was not found
     */
    public ResourceNotFoundException(String resource) {
        super(resource + " not found");
    }
}