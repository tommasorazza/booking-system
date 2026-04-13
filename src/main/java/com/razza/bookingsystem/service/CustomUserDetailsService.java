package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
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
 * It retrieves a user from the database based on their email and tenant,
 * and converts it into a CustomUserDetails object used by Spring Security.
 *
 * Responsibilities:
 * - Verify that the tenant exists
 * - Verify that the user exists within the tenant
 * - Map the user's role to a Spring Security authority
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    /**
     * Loads a user by username (email|tenantName) and maps it to CustomUserDetails.
     *
     * Behavior:
     * - Splits the username into email and tenant name
     * - Retrieves the tenant from the database
     * - Retrieves the user within that tenant
     * - Converts the user's role into a Spring Security authority
     *
     * @param username the login identifier in the format "email|tenantName"
     * @return UserDetails representation of the user for Spring Security
     * @throws UsernameNotFoundException if the tenant or user cannot be found
     */
    @Override
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
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}