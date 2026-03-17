package com.razza.bookingsystem.security;

import com.razza.bookingsystem.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

/**
 * Service for generating and validating JSON Web Tokens (JWT) for authentication.
 *
 * Uses the secret and expiration configured in {@link JwtConfig} to sign
 * and validate tokens.
 *
 * Provides methods to:
 * - Generate a JWT token for a given user email.
 * - Extract the email (subject) from a JWT token.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    /** Configuration containing the JWT secret key and expiration time */
    private final JwtConfig jwtConfig;

    /**
     * Generates a signed JWT token for the given email.
     *
     * @param email the user's email to include as the subject in the token
     * @return a signed JWT token string
     */
    public String generateToken(String email) {
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the email (subject) from the given JWT token.
     *
     * @param token the JWT token string
     * @return the email contained in the token's subject claim
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public String extractEmail(String token) {
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}