package com.razza.bookingsystem.exception;

import java.util.UUID;

public class InactivePerformanceException extends RuntimeException{
    public InactivePerformanceException(UUID performanceId){
        super("The performance: " + performanceId + " can't be scheduled because the performer deactivated it");
    }
}
