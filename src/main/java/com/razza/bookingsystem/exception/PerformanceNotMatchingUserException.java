package com.razza.bookingsystem.exception;

import java.util.UUID;

public class PerformanceNotMatchingUserException extends RuntimeException {
    public PerformanceNotMatchingUserException(UUID performanceId, String performerEmail){
        super(performanceId + " does not belong to " + performerEmail);
    }
}
