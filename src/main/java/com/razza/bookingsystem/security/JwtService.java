package com.razza.bookingsystem.security;

import com.razza.bookingsystem.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * Service responsible for handling JWT operations such as:
 * - token generation
 * - extracting claims (email, roles)
 * - validating tokens
 *
 * This implementation uses HMAC SHA-256 for signing tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    /**
     * Configuration containing JWT secret and expiration settings.
     */
    private final JwtConfig jwtConfig;

    /**
     * Generates a JWT token for the given user.
     *
     * The token contains:
     * - subject: user's email
     * - roles: list of roles without the ROLE_ prefix
     * - issued at timestamp
     * - expiration timestamp
     *
     * @param userDetails authenticated user details
     * @return signed JWT token
     */
    public String generateToken(UserDetails userDetails) {
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList();

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the email (subject) from the JWT token.
     *
     * @param token JWT token
     * @return email stored in the token
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

    /**
     * Extracts roles from the JWT token.
     *
     * Roles are stored as plain strings without the ROLE_ prefix.
     *
     * @param token JWT token
     * @return list of roles
     */
    public List<String> extractRoles(String token) {
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());

        Object roles = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles");

        return ((List<?>) roles).stream()
                .map(Object::toString)
                .toList();
    }

    /**
     * Validates the JWT token against the provided user details.
     *
     * A token is considered valid if:
     * - the email matches the user
     * - the token is not expired
     *
     * @param token JWT token
     * @param userDetails user details to validate against
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks whether the JWT token is expired.
     *
     * @param token JWT token
     * @return true if the token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        return expiration.before(new Date());
    }
}