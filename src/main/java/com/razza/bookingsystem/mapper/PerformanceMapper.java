package com.razza.bookingsystem.mapper;

import org.mapstruct.Mapper;
import com.razza.bookingsystem.domain.Performance;
import com.razza.bookingsystem.dto.PerformanceDto;

/**
 * Mapper interface for converting between {@link Performance} entities
 * and {@link PerformanceDto} objects.
 * Implemented automatically by MapStruct.
 */
@Mapper(componentModel = "spring")
public interface PerformanceMapper {

    /**
     * Converts a {@link Performance} entity to a {@link PerformanceDto}.
     *
     * @param Performance the Performance entity
     * @return the corresponding PerformanceDto
     */
    PerformanceDto toDto(Performance performance);

    /**
     * Converts a {@link PerformanceDto} to a {@link Performance} entity.
     *
     * @param dto the Performance DTO
     * @return the corresponding Performance entity
     */
    Performance toEntity(PerformanceDto dto);
}