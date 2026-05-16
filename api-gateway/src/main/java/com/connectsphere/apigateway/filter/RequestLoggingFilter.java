package com.connectsphere.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global request/response logging filter.
 * Logs method, path, status, and duration for every request.
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String requestId = request.getId();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        log.info("[{}] --> {} {}", requestId, method, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] <-- {} {} | Status: {} | Duration: {}ms",
                    requestId, method, path,
                    response.getStatusCode(),
                    duration);
        }));
    }

    @Override
    public int getOrder() {
        return -2; // Run before GlobalAuthFilter
    }
}