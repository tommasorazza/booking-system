package com.razza.bookingsystem.mapper;

import org.mapstruct.Mapper;
import com.razza.bookingsystem.domain.Availability;
import com.razza.bookingsystem.dto.AvailabilityDto;

/**
 * Mapper interface for converting between {@link Availability} entities
 * and {@link AvailabilityDto} objects.
 * Implemented automatically by MapStruct.
 */
@Mapper(componentModel = "spring")
public interface AvailabilityMapper {

    /**
     * Converts an {@link Availability} entity to an {@link AvailabilityDto}.
     *
     * @param availability the Availability entity
     * @return the corresponding AvailabilityDto
     */
    AvailabilityDto toDto(Availability availability);

    /**
     * Converts an {@link AvailabilityDto} to an {@link Availability} entity.
     *
     * @param dto the Availability DTO
     * @return the corresponding Availability entity
     */
    Availability toEntity(AvailabilityDto dto);
}