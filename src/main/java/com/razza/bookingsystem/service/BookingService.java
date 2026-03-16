package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Booking;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.dto.BookingDto;
import com.razza.bookingsystem.mapper.BookingMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/**
 * Service responsible for managing bookings.
 * Handles creation and cancellation of bookings, capacity updates,
 * and ensures business rules such as seat availability and concurrency safety.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final BookingMapper bookingMapper;

    /**
     * Creates a new booking for a user for a given event.
     * Reduces the available capacity of the event and ensures
     * the user does not already have a booking for the same event.
     *
     * @param userId the UUID of the user making the booking
     * @param eventId the UUID of the event to book
     * @param quantity the number of seats to book
     * @return the confirmed BookingDto
     * @throws RuntimeException if the event is not found,
     *                          if there are not enough seats available,
     *                          or if the user already has a booking
     */
    @Transactional
    public BookingDto createBooking(UUID userId, UUID eventId, int quantity) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getAvailableCapacity() < quantity) {
            throw new RuntimeException("Not enough seats available");
        }

        if (bookingRepository.findByUserIdAndEventId(userId, eventId).isPresent()) {
            throw new RuntimeException("User already has a booking for this event");
        }

        event.setAvailableCapacity(event.getAvailableCapacity() - quantity);
        eventRepository.save(event);

        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .eventId(eventId)
                .quantity(quantity)
                .status(com.razza.bookingsystem.domain.Status.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toDto(saved);
    }

    /**
     * Cancels an existing booking.
     * Restores the event's available capacity and marks the booking as cancelled.
     *
     * @param bookingId the UUID of the booking to cancel
     * @throws RuntimeException if the booking or event is not found,
     *                          or if the booking is already cancelled
     */
    @Transactional
    public void cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == com.razza.bookingsystem.domain.Status.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        Event event = eventRepository.findById(booking.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setAvailableCapacity(event.getAvailableCapacity() + booking.getQuantity());
        eventRepository.save(event);

        booking.setStatus(com.razza.bookingsystem.domain.Status.CANCELLED);
        bookingRepository.save(booking);
    }

    /**
     * Retrieves all bookings made by a specific user.
     *
     * @param userId the UUID of the user
     * @return list of BookingDto objects
     */
    public List<BookingDto> getUserBookings(UUID userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }
}