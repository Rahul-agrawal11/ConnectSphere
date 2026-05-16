package com.connectsphere.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingFilterTest {

    @Test
    void filter_ShouldCallNextFilter() {
        RequestLoggingFilter filter = new RequestLoggingFilter();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/posts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        AtomicBoolean chainCalled = new AtomicBoolean(false);

        GatewayFilterChain chain = webExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertTrue(chainCalled.get());
    }

    @Test
    void getOrder_ShouldReturnMinusTwo() {
        RequestLoggingFilter filter = new RequestLoggingFilter();

        assertEquals(-2, filter.getOrder());
    }
}