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
     * Converts a {@link Availability} entity to a {@link AvailabilityDto}.
     *
     * @param Availability the Availability entity
     * @return the corresponding AvailabilityDto
     */
    AvailabilityDto toDto(Availability availability);

    /**
     * Converts a {@link AvailabilityDto} to a {@link Availability} entity.
     *
     * @param dto the Availability DTO
     * @return the corresponding Availability entity
     */
    Availability toEntity(AvailabilityDto dto);
}