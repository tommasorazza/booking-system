package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Venue;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.repository.VenueRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 *
 * This service is responsible for loading user-specific data during authentication.
 * It retrieves a user from the database based on their email and venue,
 * and converts it into a CustomUserDetails object used by Spring Security.
 *
 * Responsibilities:
 * - Verify that the venue exists
 * - Verify that the user exists within the venue
 * - Map the user's role to a Spring Security authority
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    /**
     * Loads a user by username (email|venueName) and maps it to CustomUserDetails.
     *
     * Behavior:
     * - Splits the username into email and venue name
     * - Retrieves the venue from the database
     * - Retrieves the user within that venue
     * - Converts the user's role into a Spring Security authority
     *
     * @param username the login identifier in the format "email|venueName"
     * @return UserDetails representation of the user for Spring Security
     * @throws UsernameNotFoundException if the venue or user cannot be found
     */
    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String[] parts = username.split("\\|");
        String email = parts[0];
        String venueName = parts[1];
        Venue venue = venueRepository.findByName(venueName)
                .orElseThrow(() -> new UsernameNotFoundException("venue: " + venueName + " not found"));
        User user = userRepository.findByEmailAndVenue(email, venue)
                .orElseThrow(() -> new UsernameNotFoundException("user: " + email + " not found"));

        return new CustomUserDetails(
                user.getId(),
                user.getName(),
                user.getBirthDate(),
                user.getEmail(),
                user.getPassword(),
                user.getVenue(),
                user.getAvailability(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}