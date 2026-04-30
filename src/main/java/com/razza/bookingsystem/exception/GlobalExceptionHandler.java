package com.razza.bookingsystem.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Global exception handler for the Booking System API.
 *
 * This class intercepts exceptions and returns
 * consistent HTTP responses with appropriate status codes and messages.
 *
 * Specific exceptions are mapped to HTTP status codes:
 * - BookingAlreadyCancelledException, BookingAlreadyPresentException,
 *   NotEnoughSeatsException, EventDeleteException, EventDecreaseException,
 *   EmailAlreadyExistsException, UserAlreadyExistsException → 409 CONFLICT
 * - ResourceNotFoundException → 404 NOT FOUND
 * - AccessDeniedException → 403 FORBIDDEN
 * - AuthenticationException → 401 UNAUTHORIZED
 * - Other uncaught exceptions → 500 INTERNAL SERVER ERROR
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles BookingAlreadyCancelledException and returns 409 CONFLICT.
     */
    @ExceptionHandler(BookingAlreadyCancelledException.class)
    public ResponseEntity<Map<String, Object>> handleBookingAlreadyCancelled(BookingAlreadyCancelledException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles BookingAlreadyPresentException and returns 409 CONFLICT.
     */
    @ExceptionHandler(BookingAlreadyPresentException.class)
    public ResponseEntity<Map<String, Object>> handleBookingAlreadyPresent(BookingAlreadyPresentException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles NotEnoughSeatsException and returns 409 CONFLICT.
     */
    @ExceptionHandler(NotEnoughSeatsException.class)
    public ResponseEntity<Map<String, Object>> handleNotEnoughSeats(NotEnoughSeatsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles EventDeleteException and returns 409 CONFLICT.
     */
    @ExceptionHandler(EventDeleteException.class)
    public ResponseEntity<Map<String, Object>> handleEventDelete(EventDeleteException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles UserDeleteException and returns 409 CONFLICT.
     */
    @ExceptionHandler(UserDeleteException.class)
    public ResponseEntity<Map<String, Object>> handleUserDelete(UserDeleteException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles EventDecreaseException and returns 409 CONFLICT.
     */
    @ExceptionHandler(EventDecreaseException.class)
    public ResponseEntity<Map<String, Object>> handleEventDecrease(EventDecreaseException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles EmailAlreadyExistsException and returns 409 CONFLICT.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles UserAlreadyExistsException and returns 409 CONFLICT.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles PastEventException and returns 409 CONFLICT.
     */
    @ExceptionHandler(PastEventException.class)
    public ResponseEntity<Map<String, Object>> handleDate(PastEventException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles data integrity violation exceptions.
     * Returns 409 CONFLICT if it's a duplicate booking exception,
     * Otherwise 500 INTERNAL SERVER ERROR.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {

        if (isUniqueBookingConstraint(ex)) {
            return buildResponse(HttpStatus.CONFLICT, "Booking already exists for this user and event");
        }

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "database error");
    }

    /**
     * Helper method to determine if an exception is due to same user booking the same event twice.
    */
    private boolean isUniqueBookingConstraint(DataIntegrityViolationException ex) {
        Throwable cause = ex.getRootCause();

        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage().contains("booking_user_event_unique");
        }
    
        return false;
    }

    /**
     * Handles ResourceNotFoundException and returns 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles AccessDeniedException and returns 403 FORBIDDEN.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Handles QuantityException and returns 400 BAD_REQUEST.
     */
    @ExceptionHandler(QuantityException.class)
    public ResponseEntity<Map<String, Object>> handleQuantity(QuantityException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles IllegalArgumentException and returns 400 BAD_REQUEST.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles generic exceptions.
     * Returns 401 UNAUTHORIZED if it's an AuthenticationException,
     * otherwise 500 INTERNAL SERVER ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        if (ex instanceof AuthenticationException) {
            return buildResponse(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error occurred");
    }

    /**
     * Builds a standard error response containing timestamp, HTTP status code, and message.
     *
     * @param status  the HTTP status code to return
     * @param message the error message
     * @return a ResponseEntity containing the error details
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "timestamp", OffsetDateTime.now(),
                        "status", status.value(),
                        "error", message
                ));
    }
}