package com.connectsphere.post.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Post-service security configuration.
 *
 * This service sits BEHIND the API Gateway. The gateway has already:
 *   1. Validated the JWT
 *   2. Injected X-User-Id and X-User-Role headers
 *
 * So this service does NOT need a JWT filter. It simply reads the
 * injected headers from request parameters in controller methods.
 *
 * All routes are permitted at the Spring Security level — access control
 * is enforced in the service layer (ownership checks) and via @PreAuthorize
 * for admin routes.
 *
 * In production, add network-level rules so port 8082 is NOT
 * accessible from outside the cluster — only the gateway can reach it.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public read endpoints — no auth needed
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/posts/public",
                                "/api/v1/posts/search",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/api-docs/**"
                        ).permitAll()
                        // All others permitted here — business logic enforces access
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}