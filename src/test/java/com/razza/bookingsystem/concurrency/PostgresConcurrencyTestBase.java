package com.razza.bookingsystem.concurrency;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=30"
})
public abstract class PostgresConcurrencyTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();   // singleton: stays up for the whole test run, Ryuk cleans it up
    }
}
