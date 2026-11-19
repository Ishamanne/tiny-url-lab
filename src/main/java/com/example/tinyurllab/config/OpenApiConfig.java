package com.example.tinyurllab.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "basicAuth";

        return new OpenAPI()
                .openapi("3.0.1")
                .info(new Info()
                        .title("URL Shortener API")
                        .version("2.0")
                        .description("""
                                API for creating and managing shortened URLs with caching and metrics support.

                                Features:
                                - Create, retrieve, and delete shortened URLs
                                - Analytics tracking for URL clicks
                                - Caching for improved performance
                                - Metrics monitoring via /actuator/metrics
                                - Prometheus metrics at /actuator/prometheus

                                Authentication:
                                - USER role: Can view URLs and analytics
                                - ADMIN role: Can create and delete URLs
                                """)
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Basic Authentication - Use 'user:user123' or 'admin:admin123'")));
    }
}
