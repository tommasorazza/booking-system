package com.razza.bookingsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for customizing the OpenAPI (Swagger) documentation.
 *
 * This configuration defines a global security scheme using JWT Bearer tokens.
 * It ensures that all API endpoints require authentication unless explicitly
 * overridden.
 *
 * The configured security scheme will appear in Swagger UI, allowing users
 * to authorize requests by providing a valid JWT token.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates and configures the OpenAPI definition with a JWT Bearer security scheme.
     *
     * The security scheme:
     *
     *  Uses HTTP authentication
     *  Applies the "bearer" scheme
     *  Specifies JWT as the token format
     *
     * All endpoints will require this security scheme by default.
     *
     * @return configured {@link OpenAPI} instance with security settings
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // Apply security requirement globally
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                // Define security scheme in components
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}