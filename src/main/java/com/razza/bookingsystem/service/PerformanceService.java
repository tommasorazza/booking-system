package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.PerformanceDto;
import com.razza.bookingsystem.dto.ScheduledPerformanceDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.mapper.PerformanceMapper;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.PerformanceRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

import static com.razza.bookingsystem.domain.Role.PERFORMER;

/**
 * Service responsible for managing performances.
 *
 * Handles:
 *  Retrieving the scheduled performances of a performer
 *  Adding performances
 *  Deactivating performances
 *  Modifying performances
 *
 * Enforces venue isolation, so users can only work with performances of their own venue.
 */
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final EventRepository eventRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;
    private final PerformanceMapper performanceMapper;

    /** Timezone used to show the start time of the performances, read from the app configuration. */
    @Value("${app.timezone}")   // ${} -> application property placeholder
    private String timezone;

    /**
     * Retrieves the upcoming scheduled performances of a performer.
     *
     * Admins can retrieve the performances of any user within the venue.
     * Other users only get their own, and they must be performers.
     * Only events in the future that are not canceled are considered,
     * and start times are converted to the configured timezone.
     *
     * @param userId identifier of the target user (used only by admins)
     * @param currentUserId identifier of the requesting user
     * @param isAdmin whether the requesting user has admin privileges
     * @param venue venue of the requesting user
     * @return list of ScheduledPerformanceDto
     * @throws ResourceNotFoundException if the user does not exist within the venue
     */
    @Transactional
    public List<ScheduledPerformanceDto> getPerformances(UUID userId, UUID currentUserId, Boolean isAdmin, Venue venue) {

        User user;

        if(isAdmin){
            user = userRepository.findByIdAndVenueId(userId, venue.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("user", userId));
        } else {
            user = userRepository.findByIdAndVenueAndRole(currentUserId, venue, PERFORMER)
                    .orElseThrow(() -> new ResourceNotFoundException("user", userId));
        }

        return eventRepository.findByVenue(user.getVenue())
                .stream()
                .filter(event -> event.getDate().isAfter(OffsetDateTime.now()))
                .filter(event -> event.getStatus() != Status.CANCELLED)
                .filter(event -> event.getSchedule().stream()
                        .anyMatch(timeSlot -> userRepository.findByEmailAndVenue(timeSlot.getUserEmail(), user.getVenue())      // list.stream().anyMatch(listElement -> condition), if the condition evaluates to true at least once, the whole list is kept, so in my case if event has got at least one timeslot covered by the user, the whole event is kept
                                .map(u -> u.getId().equals(user.getId()))
                                .orElse(false)))
                .flatMap(event -> event.getSchedule().stream()
                        .filter(timeSlot -> Objects.equals(timeSlot.getUserEmail(), user.getEmail()))
                        .flatMap(timeSlot -> performanceRepository.findById(timeSlot.getPerformanceId()).stream().map(performance -> new ScheduledPerformanceDto(performance.getName(), performance.getDescription(), performance.getDuration(), performance.getPerformanceType(), timeSlot.getStartTime().atZoneSameInstant(ZoneId.of(timezone)).toOffsetDateTime(), event.getLocation()))))        // this line is of the kind: flatMap(element -> methodThatReturnsAnOptional.stream().map(...)), the first flatMap is needed because the optional.stream() could be empty, also flatMap requires a stream() at the right of the arrow, this stream will either be empty or contain one element (the performance)
                .toList();
    }

    /**
     * Adds a new performance to a user.
     *
     * Admins can add a performance to any user within the venue.
     * Other users can only add it to themselves, and they must be performers.
     *
     * @param performanceDto data of the performance to add
     * @param userId identifier of the target user (used only by admins)
     * @param currentUserId identifier of the requesting user
     * @param isAdmin whether the requesting user has admin privileges
     * @param venue venue of the requesting user
     * @throws ResourceNotFoundException if the user does not exist within the venue
     */
    @Transactional
    public void addPerformance(PerformanceDto performanceDto, UUID userId, UUID currentUserId, Boolean isAdmin, Venue venue){

        User user;

        if(isAdmin){
            user = userRepository.findByIdAndVenueId(userId, venue.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("user", userId));
        } else {
            user = userRepository.findByIdAndVenueAndRole(currentUserId, venue, PERFORMER)
                    .orElseThrow(() -> new ResourceNotFoundException("user", userId));
        }

        Performance performance;

        performance = Performance.builder()
                .user(user)
                .name(performanceDto.getName())
                .description(performanceDto.getDescription())
                .duration(performanceDto.getDuration())
                .performanceType(performanceDto.getPerformanceType())
                .build();

        performanceRepository.save(performance);
    }

    /**
     * Deletes a performance by marking it as INACTIVE.
     * The performance is not removed, so it can't be scheduled in new events anymore.
     *
     * @param performanceId identifier of the performance
     * @throws ResourceNotFoundException if the performance does not exist
     */
    @Transactional
    public void deletePerformance(UUID performanceId){
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new ResourceNotFoundException("performance", performanceId));

        performance.setPerformanceType(PerformanceType.INACTIVE);
    }

    /**
     * Updates the type and the duration of an existing performance.
     *
     * @param performanceId identifier of the performance
     * @param performanceType the new performance type
     * @param duration the new duration in minutes
     * @throws ResourceNotFoundException if the performance does not exist
     */
    @Transactional
    public void modifyPerformance(UUID performanceId, PerformanceType performanceType, int duration) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new ResourceNotFoundException("performance", performanceId));

        performance.setPerformanceType(performanceType);
        performance.setDuration(duration);
    }
}