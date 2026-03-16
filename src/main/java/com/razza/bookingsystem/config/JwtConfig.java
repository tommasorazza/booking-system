package com.razza.bookingsystem.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/***
 * This class provides the JWT secret key used for signing tokens
 * and the token expiration time in milliseconds.
 */
@Getter
@Component
public class JwtConfig {

    /** Secret key used to sign JWT tokens */
    @Value("${jwt.secret}")
    private String secret;

    /** JWT token expiration time in milliseconds */
    @Value("${jwt.expiration}")
    private long expiration;
}