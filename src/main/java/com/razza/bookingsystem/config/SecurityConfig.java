package com.razza.bookingsystem.config;

import com.razza.bookingsystem.security.JwtAuthenticationFilter;
import com.razza.bookingsystem.security.RateLimitingFilter;
import com.razza.bookingsystem.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Configures Spring Security for the application.
 *
 * This configuration sets up stateless JWT based authentication for the REST API.
 * It defines which endpoints are publicly accessible and ensures that all other
 * requests require authentication. The {@link JwtAuthenticationFilter} is
 * added to the security filter chain to extract and validate JWT tokens from
 * incoming requests and populate the Spring Security context. The
 * {@link RateLimitingFilter} runs right after it in the same chain.
 *
 * CSRF is disabled since the API is stateless. Swagger/OpenAPI documentation
 * is publicly accessible.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Configures the Spring Security filter chain.
     *
     * This method sets up:
     *  Disabling of CSRF protection since the API is stateless
     *  Stateless sessions and a 401 response for unauthenticated requests
     *  Authorization rules:
     *      Permit all requests to /auth/** (login and register endpoints)
     *      Permit all requests to Swagger/OpenAPI docs (/swagger-ui/** and /v3/api-docs/**)
     *      Require authentication for all other endpoints
     *  The custom JwtAuthenticationFilter, registered before
     *  UsernamePasswordAuthenticationFilter
     *  The RateLimitingFilter, registered right after the JwtAuthenticationFilter
     *
     * @param http the HttpSecurity object used to configure HTTP security
     * @return the configured SecurityFilterChain bean
     * @throws Exception if there is a configuration error
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF because the API uses JWT-based stateless authentication, CSRF means cross site request forgery, it is a safety measure against malicious attempts to access your session through the cookie info, irrelevant in this case
                .csrf(AbstractHttpConfigurer::disable)
                // Make all the HTTP requests stateless
                .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        // Returns 401 for unauthorized requests, instead of redirecting them to the login page
                ).exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value(),
                                        authException.getMessage()))
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
                // Set the authentication provider (DAO + password encoder), DAO (data access object) authentication provider is a built-in class that takes your username and calls userDetailsService -> userRepository and checks your username against hashed password to verify they match
                .authenticationProvider(authenticationProvider())
                // Add JWT filter before the default username/password authentication filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);


        return http.build();        // this returns securityFilterChain object
    }

    /**
     * Configures the authentication provider used by Spring Security.
     *
     * This method sets up a DaoAuthenticationProvider, which uses
     * the CustomUserDetailsService to load user details from the database
     * and the PasswordEncoder (BCrypt) to verify passwords.
     *
     * @return an AuthenticationProvider bean
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // Load users from DB
        provider.setPasswordEncoder(passwordEncoder);       // Verify hashed passwords
        return provider;
    }

    /**
     * Exposes the AuthenticationManager bean.
     *
     * The AuthenticationManager is responsible for processing
     * authentication requests (like login) and delegating them to the
     * configured AuthenticationProvider.
     *
     * @param config the AuthenticationConfiguration used to retrieve the authentication manager
     * @return the AuthenticationManager bean
     * @throws Exception if there is a problem creating the authentication manager
     */
    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(                     // config is an autoconfigured spring security class that knows how to assemble an authentication manager object from the already specified DAOAuthenticationProvider
                                                                                                                        org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Disables the automatic servlet registration of the RateLimitingFilter.
     *
     * Since the filter is a Spring bean, Spring Boot would register it as a
     * servlet filter on its own and it would run twice. Disabling it here means
     * it only runs inside the security filter chain.
     *
     * @param filter the RateLimitingFilter bean
     * @return a FilterRegistrationBean with the registration disabled
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

}