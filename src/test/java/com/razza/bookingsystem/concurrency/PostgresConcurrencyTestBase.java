package com.razza.bookingsystem.concurrency;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for the concurrency tests that need a real PostgreSQL database.
 *
 * Starts a single PostgreSQL container shared by all the subclasses, and
 * configures the Spring context to use it with the test profile, the PostgreSQL
 * dialect, a schema recreated at startup and dropped at shutdown, and a larger
 * connection pool to allow many concurrent requests.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=30"
})
public abstract class PostgresConcurrencyTestBase {

    /**
     * PostgreSQL container shared by all the subclasses. The connection details
     * are injected automatically into the Spring context by ServiceConnection.
     */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();   // singleton: stays up for the whole test run, Ryuk cleans it up
    }
}