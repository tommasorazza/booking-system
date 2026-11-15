package com.razza.bookingsystem.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.razza.bookingsystem.domain.Role;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * No Spring context is started — the handler is instantiated directly and each
 * handler method is called with the exception it is meant to handle.
 * Every test verifies the HTTP status, the status written in the body, the error
 * message and the presence of the timestamp.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    /**
     * Creates a fresh GlobalExceptionHandler before each test.
     */
    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    /**
     * Checks that the response has the expected HTTP status, that the same status is
     * written in the body, that the error message is the expected one and that a
     * timestamp is present.
     */
    private void assertResponse(ResponseEntity<Map<String, Object>> response, HttpStatus expectedStatus, String expectedMessage) {
        assertEquals(expectedStatus.value(), response.getStatusCode().value());
        assertEquals(expectedStatus.value(), response.getBody().get("status"));
        assertEquals(expectedMessage, response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    // 409 CONFLICT

    @Transactional
    @Test
    void handleBookingAlreadyCancelled_returns409() {
        BookingAlreadyCancelledException ex = new BookingAlreadyCancelledException();

        assertResponse(handler.handleBookingAlreadyCancelled(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    @Transactional
    @Test
    void handleBookingAlreadyPresent_returns409() {
        BookingAlreadyPresentException ex = new BookingAlreadyPresentException(2);

        assertResponse(handler.handleBookingAlreadyPresent(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    @Transactional
    @Test
    void handleNotEnoughSeats_returns409() {
        NotEnoughSeatsException ex = new NotEnoughSeatsException(5);

        assertResponse(handler.handleNotEnoughSeats(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    @Transactional
    @Test
    void handleEventDelete_returns409() {
        EventDeleteException ex = new EventDeleteException(3);

        assertResponse(handler.handleEventDelete(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    @Transactional
    @Test
    void handleUserDelete_returns409() {
        UserDeleteException ex = new UserDeleteException(1);

        assertResponse(handler.handleUserDelete(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    @Transactional
    @Test
    void handleEventDecrease_returns409() {
        EventDecreaseException ex = new EventDecreaseException(4);

        assertResponse(handler.handleEventDecrease(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    @Transactional
    @Test
    void handleEmailAlreadyExists_returns409() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("user@a.com");

        assertResponse(handler.handleEmailAlreadyExists(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    @Transactional
    @Test
    void handleUserAlreadyExists_returns409() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("user@a.com");

        assertResponse(handler.handleUserAlreadyExists(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    @Transactional
    @Test
    void handleDate_pastEvent_returns409() {
        PastEventException ex = new PastEventException(OffsetDateTime.now().minusDays(1));

        assertResponse(handler.handleDate(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    @Transactional
    @Test
    void handleInactivePerformance_returns409() {
        InactivePerformanceException ex = new InactivePerformanceException(UUID.randomUUID());

        assertResponse(handler.handleInactivePerformance(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * An InvalidScheduleException created with conflict = true (for example two
     * overlapping events) must be mapped to 409 CONFLICT.
     */
    @Transactional
    @Test
    void handleInvalidSchedule_conflictTrue_returns409() {
        InvalidScheduleException ex = InvalidScheduleException.overlapping();

        assertResponse(handler.handleInvalidSchedule(ex), HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * IllegalArgumentException is currently mapped to 409 CONFLICT, even if the
     * Javadoc above the handler says 400 BAD_REQUEST.
     */
    @Transactional
    @Test
    void handleIllegalArgument_returns409() {
        IllegalArgumentException ex = new IllegalArgumentException("illegal argument");

        assertResponse(handler.handleIllegalArgument(ex), HttpStatus.CONFLICT, "illegal argument");
    }

    // 404 NOT FOUND

    @Transactional
    @Test
    void handleNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("venue", UUID.randomUUID());

        assertResponse(handler.handleNotFound(ex), HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 403 FORBIDDEN

    @Transactional
    @Test
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Access Denied");

        assertResponse(handler.handleAccessDenied(ex), HttpStatus.FORBIDDEN, "Access Denied");
    }

    // 401 UNAUTHORIZED

    @Transactional
    @Test
    void handleInvalidAge_returns401() {
        invalidAgeException ex = new invalidAgeException();

        assertResponse(handler.handleInvalidAge(ex), HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * An AuthenticationException reaching the generic handler must be mapped to
     * 401 UNAUTHORIZED with a fixed message, without leaking the original one.
     */
    @Transactional
    @Test
    void handleGeneric_authenticationException_returns401WithFixedMessage() {
        BadCredentialsException ex = new BadCredentialsException("password does not match");

        assertResponse(handler.handleGeneric(ex), HttpStatus.UNAUTHORIZED, "invalid credentials");
    }

    // 400 BAD REQUEST

    @Transactional
    @Test
    void handleQuantity_returns400() {
        QuantityException ex = new QuantityException(0);

        assertResponse(handler.handleQuantity(ex), HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * An InvalidScheduleException created with conflict = false (for example an
     * unregistered email in the schedule) must be mapped to 400 BAD_REQUEST.
     */
    @Transactional
    @Test
    void handleInvalidSchedule_conflictFalse_returns400() {
        InvalidScheduleException ex = InvalidScheduleException.unregisteredEmail("ghost@a.com");

        assertResponse(handler.handleInvalidSchedule(ex), HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @Transactional
    @Test
    void handleInvalidRole_returns400() {
        InvalidRoleException ex = InvalidRoleException.wrongSignUp();

        assertResponse(handler.handleInvalidRole(ex), HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @Transactional
    @Test
    void handleMissingAvailability_returns400() {
        MissingAvailabilityException ex = new MissingAvailabilityException();

        assertResponse(handler.handleMissingAvailability(ex), HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @Transactional
    @Test
    void handlePerformanceNotMatchingUser_returns400() {
        PerformanceNotMatchingUserException ex = new PerformanceNotMatchingUserException(UUID.randomUUID(), "performer@a.com");

        assertResponse(handler.handlePerformanceNotMatchingUser(ex), HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @Transactional
    @Test
    void handleEmptySchedule_returns400() {
        EmptyScheduleException ex = new EmptyScheduleException();

        assertResponse(handler.handleEmptySchedule(ex), HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @Transactional
    @Test
    void handleBadCapacity_returns400() {
        BadCapacityException ex = new BadCapacityException();

        assertResponse(handler.handleBadCapacity(ex), HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 BAD REQUEST — request body that can't be read

    /**
     * When the JSON contains a value that is not part of an enum, the message should
     * contain the wrong value and the name of the enum.
     */
    @Transactional
    @Test
    void handleHttpMessageNotReadable_invalidEnumValue_returns400WithValueAndEnumName() {
        InvalidFormatException cause = InvalidFormatException.from(null, "not a valid value", "WAITER", Role.class);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad body", cause, mock(HttpInputMessage.class));

        assertResponse(handler.handleHttpMessageNotReadable(ex), HttpStatus.BAD_REQUEST, "Invalid value 'WAITER' for Role");
    }

    /**
     * When the invalid value is not for an enum (for example text instead of a number),
     * the generic malformed body message is returned.
     */
    @Transactional
    @Test
    void handleHttpMessageNotReadable_invalidFormatNotEnum_returns400WithGenericMessage() {
        InvalidFormatException cause = InvalidFormatException.from(null, "not a number", "abc", Integer.class);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad body", cause, mock(HttpInputMessage.class));

        assertResponse(handler.handleHttpMessageNotReadable(ex), HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    /**
     * When the body can't be read for any other reason (no specific cause),
     * the generic malformed body message is returned.
     */
    @Transactional
    @Test
    void handleHttpMessageNotReadable_noCause_returns400WithGenericMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad body", mock(HttpInputMessage.class));

        assertResponse(handler.handleHttpMessageNotReadable(ex), HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    // 500 INTERNAL SERVER ERROR

    /**
     * Any exception without a specific handler must be mapped to 500 with a fixed
     * message, so internal details are never sent to the client.
     */
    @Transactional
    @Test
    void handleGeneric_unexpectedException_returns500WithFixedMessage() {
        RuntimeException ex = new RuntimeException("database connection lost");

        assertResponse(handler.handleGeneric(ex), HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error occurred");
    }
}