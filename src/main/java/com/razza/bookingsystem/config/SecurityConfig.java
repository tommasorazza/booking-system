package com.razza.bookingsystem.config;

import com.razza.bookingsystem.security.JwtAuthenticationFilter;
import com.razza.bookingsystem.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Configures application security using Spring Security.
 * <p>
 * This configuration sets up stateless JWT-based authentication for the API.
 * It defines which endpoints are publicly accessible and ensures that all other
 * requests require authentication.</p>
 * <p>
 * The {@link JwtAuthenticationFilter} is added to the security filter chain
 * to extract and validate JWT tokens from incoming requests and populate the
 * Spring Security authentication context.</p>
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;


    /**
     * Defines the security filter chain used by Spring Security.
     * <p>
     * Configuration includes:
     * Disabling CSRF protection since the API uses stateless JWT authentication.
     * Allowing public access to authentication endpoints.
     * Allowing public access to Swagger/OpenAPI documentation.
     * Requiring authentication for all other endpoints.
     * Registering the JWT authentication filter before the default
     * {@link UsernamePasswordAuthenticationFilter}.
     *
     * @param http the {@link HttpSecurity} configuration object used to define
     *             security behavior for HTTP requests
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if a security configuration error occurs
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF because the API uses JWT-based stateless authentication
                .csrf(AbstractHttpConfigurer::disable)
                // Make all the http requests stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Define authorization rules for incoming requests
                .authorizeHttpRequests(auth -> auth
                        // Allow unauthenticated access to authentication endpoints
                        .requestMatchers("/auth/**").permitAll()

                        // Allow access to Swagger/OpenAPI documentation
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Require authentication for all other endpoints
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                // Add JWT filter before the default authentication filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

}