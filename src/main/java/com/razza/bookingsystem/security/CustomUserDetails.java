package com.razza.bookingsystem.security;

import com.razza.bookingsystem.domain.Availability;
import com.razza.bookingsystem.domain.Venue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Custom implementation of Spring Security's UserDetails interface.
 *
 * This class represents the authenticated user within the application
 * and is used by Spring Security during authentication and authorization.
 *
 * It includes additional domain-specific information such as:
 * - unique user ID
 * - venue (for multi-venue support)
 *
 * The email is used as the username for authentication.
 */
@Getter
@Setter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    /**
     * Unique identifier of the user.
     */
    private UUID id;

    private String name;

    private OffsetDateTime birthDate;

    /**
     * User's email address, used as the username.
     */
    private String email;

    /**
     * User's hashed password.
     */
    private String password;

    /**
     * Venue associated with the user.
     * Used to enforce multi-venue isolation.
     */
    private Venue venue;

    private Availability availability;

    /**
     * Collection of granted authorities (roles/permissions).
     */
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Returns the username used for authentication.
     *
     * @return the username
     */
    @Override       //these methods have @Override for compile safety reasons, the compiler knows which method belong to customUserDetails and if customUserDetails.nonExistentMethod() is called, then an error will be thrown because the method will not be recognized, also because they are methods of an interface implementation
    public String getUsername() {
        return email + "|" + venue.getName();
    }

    /**
     * Returns the user's password.
     *
     * @return the hashed password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the authorities granted to the user.
     *
     * @return collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

}