package com.razza.bookingsystem.security;

import com.razza.bookingsystem.domain.Tenant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * Custom implementation of Spring Security's UserDetails interface.
 *
 * This class represents the authenticated user within the application
 * and is used by Spring Security during authentication and authorization.
 *
 * It includes additional domain-specific information such as:
 * - unique user ID
 * - tenant (for multi-tenant support)
 *
 * The email is used as the username for authentication.
 */
@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    /**
     * Unique identifier of the user.
     */
    private UUID id;

    /**
     * User's email address, used as the username.
     */
    private String email;

    /**
     * User's hashed password.
     */
    private String password;

    /**
     * Tenant associated with the user.
     * Used to enforce multi-tenant isolation.
     */
    private Tenant tenant;

    /**
     * Collection of granted authorities (roles/permissions).
     */
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Returns the username used for authentication.
     *
     * @return the username
     */
    @Override
    public String getUsername() {
        return email + "|" + tenant.getName();
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

    /**
     * Indicates whether the user's account has expired.
     *
     * @return true since account expiration is not currently enforced
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked.
     *
     * @return true since account locking is not currently enforced
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials have expired.
     *
     * @return true since credential expiration is not currently enforced
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled.
     *
     * @return true since all users are considered enabled by default
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}