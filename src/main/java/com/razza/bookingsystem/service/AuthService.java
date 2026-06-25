package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.AvailabilityDto;
import com.razza.bookingsystem.dto.PerformanceDto;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.exception.InvalidRoleException;
import com.razza.bookingsystem.exception.MissingAvailabilityException;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.exception.UserAlreadyExistsException;
import com.razza.bookingsystem.mapper.AvailabilityMapper;
import com.razza.bookingsystem.mapper.PerformanceMapper;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.AvailabilityRepository;
import com.razza.bookingsystem.repository.VenueRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Service responsible for authentication-related operations.
 *
 * Handles:
 * - user registration
 * - user authentication
 * - venue resolution and creation
 *
 * Passwords are hashed before being stored.
 * JWT tokens are generated after successful authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PerformanceMapper performanceMapper;
    private final AvailabilityMapper availabilityMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VenueRepository venueRepository;
    private final AvailabilityRepository availabilityRepository;

    /**
     * Registers a new user within a venue.
     *
     * Behavior:
     * - checks if the email is already in use within the venue
     * - retrieves a venue with the given name
     * - hashes the password
     * - assigns the GUEST role
     * - persists the user
     *
     * @param email unique email of the user within the venue
     * @param password raw password of the user
     * @param venueName name of the venue
     * @return created user as a DTO
     *
     * @throws UserAlreadyExistsException if a user with the same email already exists within the venue
     */
    public UserDto signup(String name, OffsetDateTime birthDate, String email, String password, Role role, String venueName, String availability, Set<PerformanceDto> performances) {

        Venue venue = venueRepository.findByName(venueName)
                .orElseThrow(() -> new ResourceNotFoundException("venue"));

        if (userRepository.findByEmailAndVenue(email, venue).isPresent()) {
            throw new UserAlreadyExistsException(email);
        }

        if(role != Role.GUEST && role != Role.PERFORMER){
            throw InvalidRoleException.wrongSignUp();
        }

        User user;

        if(role == Role.GUEST) {
            user = User.builder()
                    .name(name)
                    .birthDate(birthDate)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(Role.GUEST)
                    .venue(venue)
                    .status(Status.CONFIRMED)
                    .build();
        } else {
            if(availability == null){
                throw new MissingAvailabilityException();
            }

            Set<Performance> newPerformances = new HashSet<>();
            for(PerformanceDto performance : performances){
                newPerformances.add(performanceMapper.toEntity(performance));
            }

            user = User.builder()
                    .name(name)
                    .birthDate(birthDate)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(Role.PERFORMER)
                    .venue(venue)
                    .status(Status.CONFIRMED)
                    .availability(availabilityMapper.toEntity(availableDaysBuilder(availability)))
                    .performances(newPerformances)
                    .build();
            user.getAvailability().setUser(user);
            for(Performance performance : newPerformances){
                performance.setUser(user);
            }
            User saved = userRepository.save(user);
            return userMapper.toDto(saved);
        }
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * Behavior:
     * - builds a venue-scoped username (email + venue)
     * - validates credentials using the authentication manager
     * - extracts authenticated user details
     * - generates a signed JWT token
     *
     * @param email user email
     * @param password raw password
     * @param venueName venue name used for scoping authentication
     * @return JWT token if authentication succeeds
     *
     * @throws AuthenticationException if credentials are invalid
     */
    public String login(String email, String password, String venueName) {

        Venue venue = venueRepository.findByName(venueName).orElseThrow(() -> new ResourceNotFoundException("venue"));

        User user = userRepository.findByEmailAndVenue(email, venue).orElseThrow(() -> new ResourceNotFoundException("user"));

        if(user.getStatus().equals(Status.CANCELLED)) {
            throw new ResourceNotFoundException("user");
        }

        String username = email + "|" + venueName;

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return jwtService.generateToken(userDetails);
    }

    public AvailabilityDto availableDaysBuilder(String availability){
        AvailabilityDto days = new AvailabilityDto(false,false,false,false,false,false,false);
        if(availability.contains("monday")) {
            days.setMonday(true);
        }
        if(availability.contains("tuesday")) {
            days.setTuesday(true);
        }
        if(availability.contains("wednesday")) {
            days.setWednesday(true);
        }
        if(availability.contains("thursday")) {
            days.setThursday(true);
        }
        if(availability.contains("friday")) {
            days.setFriday(true);
        }
        if(availability.contains("saturday")) {
            days.setSaturday(true);
        }
        if(availability.contains("sunday")) {
            days.setSunday(true);
        }
        return days;
    }
}