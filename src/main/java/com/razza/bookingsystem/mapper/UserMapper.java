package com.razza.bookingsystem.mapper;

import org.mapstruct.Mapper;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.UserDto;

/**
 * Mapper interface for converting between {@link User} entities
 * and {@link UserDto} objects.
 * Implemented automatically by MapStruct.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Converts a {@link User} entity to a {@link UserDto}.
     *
     * @param user the user entity
     * @return the corresponding UserDto
     */
    UserDto toDto(User user);

    /**
     * Converts a {@link UserDto} to a {@link User} entity.
     *
     * @param dto the user DTO
     * @return the corresponding User entity
     */
    User toEntity(UserDto dto);
}