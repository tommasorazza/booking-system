package com.razza.bookingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BookingsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingsystemApplication.class, args);
    }

}
