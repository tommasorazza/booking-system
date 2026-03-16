package com.razza.bookingsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * This bean is used for hashing passwords with BCrypt and verifying them during login.
 */
@Configuration
public class PasswordConfig {

    /**
     * Creates a BCryptPasswordEncoder bean for hashing passwords.
     *
     * @return a PasswordEncoder instance using Bcrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}