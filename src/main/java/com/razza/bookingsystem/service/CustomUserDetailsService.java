package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.repository.TenantRepository;
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
 * It retrieves a user from the database and converts it into a CustomUserDetails
 * object used by Spring Security.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Repository used to fetch users from the database.
     */
    private final UserRepository userRepository;

    private final TenantRepository tenantRepository;

    /**
     * Loads a user by email and maps it to CustomUserDetails.
     * Behavior:
     * - looks up the user by email
     * - throws an exception if the user does not exist
     * - converts the user's role into a Spring Security authority
     *
     * @param email the user's email used as username
     * @return UserDetails representation of the user
     * @throws UsernameNotFoundException if no user is found with the given email
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String[] parts = username.split("\\|");
        String email = parts[0];
        String tenantName = parts[1];
        Tenant tenant = tenantRepository.findByName(tenantName)
                .orElseThrow(() -> new UsernameNotFoundException("tenant: " + tenantName + " not found"));
        User user = userRepository.findByEmailAndTenant(email, tenant)
                .orElseThrow(() -> new UsernameNotFoundException("user: " + email + " not found"));

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getTenant(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
