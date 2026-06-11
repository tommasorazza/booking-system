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

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final EventRepository eventRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;
    private final PerformanceMapper performanceMapper;

    @Value("{app.timezone}")
    private String timezone;

    @Transactional
    public List<ScheduledPerformanceDto> getPerformances(CustomUserDetails user) {
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

    @Transactional
    public void addPerformance(PerformanceDto performanceDto, String email, Venue venue){

        User user = userRepository.findByEmailAndVenueAndRole(email, venue, PERFORMER)
                .orElseThrow(() -> new ResourceNotFoundException("user"));

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

    @Transactional
    public void deletePerformance(UUID performanceId){
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new ResourceNotFoundException("performance", performanceId));

        performance.setPerformanceType(PerformanceType.INACTIVE);
    }

    @Transactional
    public void restorePerformance(UUID performanceId, PerformanceType performanceType) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new ResourceNotFoundException("performance", performanceId));

        performance.setPerformanceType(performanceType);
    }
}
