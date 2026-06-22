package com.razza.bookingsystem.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(InactivePerformanceException.class)
    public ResponseEntity<Map<String, Object>> handleInactivePerformance(InactivePerformanceException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
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

    @ExceptionHandler(InvalidScheduleException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSchedule(InvalidScheduleException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRole(InvalidRoleException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingAvailabilityException.class)
    public ResponseEntity<Map<String, Object>> handleMissingAvailability(MissingAvailabilityException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PerformanceNotMatchingUserException.class)
    public ResponseEntity<Map<String, Object>> handlePerformanceNotMatchingUser(PerformanceNotMatchingUserException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EmptyScheduleException.class)
    public ResponseEntity<Map<String, Object>> handleEmptySchedule(EmptyScheduleException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BadCapacityException.class)
    public ResponseEntity<Map<String, Object>> handleBadCapacity(BadCapacityException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {   // spring exception thrown when JSON can't deserialize the body
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) { //.getTargetType() returns the java Class that JSON is failing to deserialize
                String invalidValue = ife.getValue().toString(); //.getValue().toString() returns the actual value that JSON is failing to deserialize
                String enumName = ife.getTargetType().getSimpleName(); // Class name (Role in this case)
                return buildResponse(HttpStatus.BAD_REQUEST, "Invalid value '" + invalidValue + "' for " + enumName);
            }
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body"); //should the problem be somewhere else
    }

    /**
     * Handles IllegalArgumentException and returns 400 BAD_REQUEST.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(invalidAgeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAge(invalidAgeException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
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