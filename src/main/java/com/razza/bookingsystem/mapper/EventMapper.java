package com.razza.bookingsystem.mapper;

import org.mapstruct.Mapper;
import com.razza.bookingsystem.domain.Event;
import com.razza.bookingsystem.dto.EventDto;

/**
 * Mapper interface for converting between {@link Event} entities
 * and {@link EventDto} objects.
 * Implemented automatically by MapStruct.
 */
@Mapper(componentModel = "spring")
public interface EventMapper {

    /**
     * Converts an {@link Event} entity to an {@link EventDto}.
     *
     * @param event the event entity
     * @return the corresponding EventDto
     */
    EventDto toDto(Event event);

    /**
     * Converts an {@link EventDto} to an {@link Event} entity.
     *
     * @param dto the event DTO
     * @return the corresponding Event entity
     */
    Event toEntity(EventDto dto);
}