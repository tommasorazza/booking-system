package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.PerformanceDto;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.exception.UserDeleteException;
import com.razza.bookingsystem.mapper.AvailabilityMapper;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.AvailabilityRepository;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.PerformanceRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.razza.bookingsystem.domain.Role.PERFORMER;
import static java.util.stream.Collectors.toList;
import static org.hibernate.Hibernate.list;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final AvailabilityMapper availabilityMapper;
    /**
     * Promotes an existing user to ADMIN within a specific venue.
     *
     * The method:
     * - retrieves the user scoped to the given venue
     * - updates the role to ADMIN
     * - persists the updated user entity
     *
     * @param userId identifier of the user to promote
     * @param venue venue to which the user belongs
     * @return updated user as a DTO
     *
     * @throws ResourceNotFoundException if the user is not found in the given venue
     */
    public UserDto makeAdmin(UUID userId, Venue venue) {

        User user = userRepository.findByIdAndVenueId(userId, venue.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));

        user.setRole(Role.ADMIN);

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    /**
     * Deletes a user if they have no active confirmed bookings.
     *
     * Rules:
     * - user must exist within the given venue
     * - user cannot have active CONFIRMED bookings
     *
     * @param userId identifier of the user to delete
     * @param venue venue to which the user belongs
     *
     * @throws ResourceNotFoundException if the user is not found in the venue
     * @throws UserDeleteException if the user has active confirmed bookings
     */
    public void deleteUser(UUID userId, Venue venue){
        User user = userRepository.findByIdAndVenueId(userId, venue.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));

        int activeBookings = bookingRepository.countByUserIdAndStatus(userId, Status.CONFIRMED);

        if (activeBookings > 0){
            throw new UserDeleteException(activeBookings);
        }



        user.setStatus(Status.CANCELLED);
        userRepository.save(user);
    }

    public List<UserDto> getAvailablePerformersByDate(LocalDate date, UUID venueId){
        if(date == null) {
            return userRepository.findByRoleAndVenueId(Role.PERFORMER, venueId)
                    .stream().map(userMapper::toDto).toList();
        } else {
            return switch (date.getDayOfWeek()) {
                case MONDAY -> userRepository.findByRoleAndVenueId(Role.PERFORMER, venueId)
                        .stream().filter(user -> {
                            return user.getAvailability().getMonday().equals(Boolean.TRUE);
                        })
                        .map(userMapper::toDto).toList();
                case TUESDAY -> userRepository.findByRoleAndVenueId(Role.PERFORMER, venueId)
                        .stream().filter(user -> {
                            return user.getAvailability().getTuesday().equals(Boolean.TRUE);
                        })
                        .map(userMapper::toDto).toList();
                case WEDNESDAY -> userRepository.findByRoleAndVenueId(Role.PERFORMER, venueId)
                        .stream().filter(user -> {
                            return user.getAvailability().getWednesday().equals(Boolean.TRUE);
                        })
                        .map(userMapper::toDto).toList();
                case THURSDAY -> userRepository.findByRoleAndVenueId(Role.PERFORMER, venueId)
                        .stream().filter(user -> {
                            return user.getAvailability().getThursday().equals(Boolean.TRUE);
                        })
                        .map(userMapper::toDto).toList();
                case FRIDAY -> userRepository.findByRoleAndVenueId(Role.PERFORMER, venueId)
                        .stream().filter(user -> {
                            return user.getAvailability().getFriday().equals(Boolean.TRUE);
                        })
                        .map(userMapper::toDto).toList();
                case SATURDAY -> userRepository.findByRoleAndVenueId(Role.PERFORMER, venueId)
                        .stream().filter(user -> {
                            return user.getAvailability().getSaturday().equals(Boolean.TRUE);
                        })
                        .map(userMapper::toDto).toList();
                case SUNDAY -> userRepository.findByRoleAndVenueId(Role.PERFORMER, venueId)
                        .stream().filter(user -> {
                            return user.getAvailability().getSunday().equals(Boolean.TRUE);
                        })
                        .map(userMapper::toDto).toList();
                default -> null;
            };
        }
    }

    public UserDto getPerformer(UUID performerId, Venue venue, Role role){
        User user = userRepository.findByIdAndVenueAndRole(performerId, venue, role)
                .orElseThrow(() -> new ResourceNotFoundException("user", performerId));

        return userMapper.toDto(user);
    }

    public void modifyAvailability(String availability, CustomUserDetails user){
        User currentUser = userRepository.findByEmailAndVenue(user.getEmail(), user.getVenue())
                .orElseThrow(() -> new ResourceNotFoundException("user", user.getId()));

        if (availability.contains("monday")) {
            currentUser.getAvailability().setMonday(true);
        } else {
            currentUser.getAvailability().setMonday(false);
        }
        if (availability.contains("tuesday")) {
            currentUser.getAvailability().setTuesday(true);
        } else {
            currentUser.getAvailability().setTuesday(false);
        }
        if (availability.contains("wednesday")) {
            currentUser.getAvailability().setWednesday(true);
        } else {
            currentUser.getAvailability().setWednesday(false);
        }
        if (availability.contains("thursday")) {
            currentUser.getAvailability().setThursday(true);
        } else {
            currentUser.getAvailability().setThursday(false);
        }
        if (availability.contains("friday")) {
            currentUser.getAvailability().setFriday(true);
        } else {
            currentUser.getAvailability().setFriday(false);
        }
        if (availability.contains("saturday")) {
            currentUser.getAvailability().setSaturday(true);
        } else {
            currentUser.getAvailability().setSaturday(false);
        }
        if (availability.contains("sunday")) {
            currentUser.getAvailability().setSunday(true);
        } else {
            currentUser.getAvailability().setSunday(false);
        }

        userRepository.save(currentUser);
    }

}