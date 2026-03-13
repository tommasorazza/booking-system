package com.razza.bookingsystem.mapper;

import org.mapstruct.Mapper;
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
     *
     * @param booking the booking entity
     * @return the corresponding BookingDto
     */
    BookingDto toDto(Booking booking);

    /**
     * Converts a {@link BookingDto} to a {@link Booking} entity.
     *
     * @param dto the booking DTO
     * @return the corresponding Booking entity
     */
    Booking toEntity(BookingDto dto);
}