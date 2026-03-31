package com.razza.bookingsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global exception handler for the application.
 *
 * Handles exceptions thrown by controllers and services
 * and converts them into standardized HTTP responses.
 *
 * Provides:
 * - consistent error structure
 * - centralized error handling
 * - simplified controller logic
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all RuntimeException instances.
     *
     * Behavior:
     * - returns HTTP 400 status
     * - includes timestamp of the error
     * - includes error message from the exception
     *
     * @param ex the thrown RuntimeException
     * @return response entity containing error details
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "error", ex.getMessage()
                ));
    }
}