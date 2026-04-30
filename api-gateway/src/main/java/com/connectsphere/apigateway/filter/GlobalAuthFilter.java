package com.connectsphere.apigateway.filter;

import com.connectsphere.apigateway.config.AppGatewayProperties;
import com.connectsphere.apigateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Global Authentication Filter.
 *
 * Runs on every request before routing.
 * - Skips JWT check for public routes
 * - Validates JWT for all protected routes
 * - Injects X-User-Id, X-User-Role, X-Username headers for downstream services
 *
 * Downstream services trust these headers instead of re-validating JWT.
 * This is safe because the gateway is the only entry point.
 */
@Slf4j
@Component
public class GlobalAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final List<String> publicRoutes;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public GlobalAuthFilter(JwtUtil jwtUtil, AppGatewayProperties appGatewayProperties) {
        this.jwtUtil = jwtUtil;
        this.publicRoutes = appGatewayProperties.getPublicRoutes();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Incoming request: {} {}", request.getMethod(), path);

        if(isPublicRoute(path)) {
            log.debug("Public route: - skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return writeUnauthorizedResponse(exchange,"Authorized Token is required");
        }
        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return writeUnauthorizedResponse(exchange, "Invalid or expired token");
        }

        // Extract claims and inject downstream headers
        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        String username = jwtUtil.extractUsername(token);

        if (userId == null || userId.isBlank()) {
            log.warn("JWT token does not contain valid userId for path: {}", path);
            return writeUnauthorizedResponse(exchange, "Invalid token: userId missing");
        }

        log.debug("Authenticated user: id={}, role={}, path={}", userId, role, path);

        // Mutate request to add identity headers for downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId != null ? userId : "")
                .header("X-User-Role", role != null ? role : "")
                .header("X-User-Username", username != null ? username : "")
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Order = -1 ensures this filter runs before Spring Cloud Gateway's
     * built-in routing filter (order = Integer.MIN_VALUE to 0 range).
     */
    @Override
    public int getOrder() {
        return -1;
    }

    /**
     * Check if the request path matches any configured public route.
     */
    private boolean isPublicRoute(String path) {
        return publicRoutes.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Write a 401 Unauthorized response with a JSON error body.
     */
    private Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                LocalDateTime.now(), message
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
