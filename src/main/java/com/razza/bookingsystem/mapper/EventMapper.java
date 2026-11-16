package com.razza.bookingsystem.mapper;

import com.razza.bookingsystem.dto.EventRequestDto;
import com.razza.bookingsystem.dto.EventResponseDto;
import org.mapstruct.Mapper;
import com.razza.bookingsystem.domain.Event;
import org.mapstruct.Mapping;

/**
 * Mapper interface for converting from {@link EventRequestDto} objects
 * to {@link Event} entities
 * and from {@link Event} entities
 * to {@link EventResponseDto} objects.
 * Implemented automatically by MapStruct.
 */
@Mapper(componentModel = "spring")
public interface EventMapper {

    /**
     * Converts an {@link Event} entity to an {@link EventResponseDto}.
     * Maps the nested {@code bookingPolicy.totalCapacity} and {@code bookingPolicy.availableCapacity}
     * to {@code totalCapacity} and {@code availableCapacity}.
     *
     * @param event the event entity
     * @return the corresponding EventResponseDto
     */
    @Mapping(source = "bookingPolicy.totalCapacity", target = "totalCapacity")
    @Mapping(source = "bookingPolicy.availableCapacity", target = "availableCapacity")
    EventResponseDto toDto(Event event);

    /**
     * Converts an {@link EventRequestDto} to an {@link Event} entity.
     *
     * @param dto the event request DTO
     * @return the corresponding Event entity
     */
    Event toEntity(EventRequestDto dto);
}