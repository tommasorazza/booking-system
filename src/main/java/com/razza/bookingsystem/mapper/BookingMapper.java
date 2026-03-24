package com.razza.bookingsystem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.razza.bookingsystem.domain.Booking;
import com.razza.bookingsystem.dto.BookingDto;

/**
 * Mapper interface for converting between {@link Booking} entities
 * and {@link BookingDto} objects.
 * Implemented automatically by MapStruct.
 */
@Mapper(componentModel = "spring")
public interface BookingMapper {

    /**
     * Converts a {@link Booking} entity to a {@link BookingDto}.
     * Maps nested {@code event.id} and {@code event.name}
     * to {@code eventId} and {@code eventName}.
     *
     * @param booking the booking entity
     * @return the corresponding BookingDto
     */
    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "event.name", target = "eventName")
    BookingDto toDto(Booking booking);

    /**
     * Converts a {@link BookingDto} to a {@link Booking} entity.
     *
     * Note: this does not automatically reconstruct the {@code Event}
     * object from {@code eventId}.
     *
     * @param dto the booking DTO
     * @return the corresponding Booking entity
     */
    Booking toEntity(BookingDto dto);
}