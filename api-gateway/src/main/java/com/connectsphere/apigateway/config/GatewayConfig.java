package com.connectsphere.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic gateway route configuration.
 *
 * Routes here supplement those in application.yml.
 * Use this for routes that need dynamic predicates or complex filters.
 *
 * For ConnectSphere, primary routing is in application.yml for clarity.
 * This class adds the actuator health-check route passthrough.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // Actuator health check — no auth required
                .route("actuator-health", route -> route
                        .path("/actuator/**")
                        .filters(filter -> filter
                                .addResponseHeader("X-Gateway", "ConnectSphere-Gateway")
                        )
                        .uri("no://op") // Handled by gateway itself
                )

                .build();
    }
}