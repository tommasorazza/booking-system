package com.razza.bookingsystem.security;

import com.razza.bookingsystem.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * Service responsible for handling JWT operations such as:
 * - token generation
 * - extracting claims (email|tenantName, role)
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
     * - subject: "email|tenantName"
     * - role: single user role without the ROLE_ prefix
     * - issued at timestamp
     * - expiration timestamp
     *
     * @param userDetails authenticated user details
     * @return signed JWT token
     */
    public String generateToken(UserDetails userDetails) {
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the subject (email|tenantName) from the JWT token.
     *
     * @param token JWT token
     * @return subject stored in the token
     */
    public String extractSubject(String token) {
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Extracts the role from the JWT token and converts it to a list of authorities.
     *
     * Even though only a single role is stored in the token, it is returned
     * as a list to comply with Spring Security expectations.
     *
     * @param token JWT token
     * @return list containing a single GrantedAuthority
     */
    public List<SimpleGrantedAuthority> extractRoles(String token) {
        Key key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());

        Object role = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");

        String roleString = (String) role;

        return List.of(new SimpleGrantedAuthority("ROLE_" + roleString));
    }

    /**
     * Validates the JWT token against the provided user details.
     *
     * A token is considered valid if:
     * - the subject matches the user (email|tenantName)
     * - the token is not expired
     *
     * @param token JWT token
     * @param userDetails user details to validate against
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String subject = extractSubject(token);
        return subject.equals(userDetails.getUsername()) && !isTokenExpired(token);
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