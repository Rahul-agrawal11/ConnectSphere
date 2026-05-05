package com.connectsphere.apigateway.filter;

import com.connectsphere.apigateway.config.AppGatewayProperties;
import com.connectsphere.apigateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalAuthFilterTest {

    private JwtUtil jwtUtil;
    private GlobalAuthFilter globalAuthFilter;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);

        AppGatewayProperties properties = new AppGatewayProperties();
        properties.setPublicRoutes(List.of(
                "/api/v1/auth/**",
                "/oauth2/**",
                "/login/**",
                "/actuator/health"
        ));

        globalAuthFilter = new GlobalAuthFilter(jwtUtil, properties);
    }

    @Test
    void filter_ShouldSkipJwtValidation_ForPublicRoute() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);

        GatewayFilterChain chain = webExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        globalAuthFilter.filter(exchange, chain).block();

        assertTrue(chainCalled.get());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filter_ShouldReturnUnauthorized_WhenAuthorizationHeaderMissing() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/posts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        globalAuthFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_ShouldReturnUnauthorized_WhenAuthorizationHeaderMalformed() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Token abc")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        globalAuthFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_ShouldReturnUnauthorized_WhenTokenIsInvalid() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

        globalAuthFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_ShouldReturnUnauthorized_WhenUserIdMissingInToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn("");

        globalAuthFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_ShouldAddUserHeaders_WhenTokenIsValid() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn("10");
        when(jwtUtil.extractRole("valid-token")).thenReturn("USER");
        when(jwtUtil.extractUsername("valid-token")).thenReturn("rahul");

        AtomicBoolean chainCalled = new AtomicBoolean(false);

        GatewayFilterChain chain = webExchange -> {
            chainCalled.set(true);

            assertEquals("10", webExchange.getRequest().getHeaders().getFirst("X-User-Id"));
            assertEquals("USER", webExchange.getRequest().getHeaders().getFirst("X-User-Role"));
            assertEquals("rahul", webExchange.getRequest().getHeaders().getFirst("X-User-Username"));

            return Mono.empty();
        };

        globalAuthFilter.filter(exchange, chain).block();

        assertTrue(chainCalled.get());
    }

    @Test
    void getOrder_ShouldReturnMinusOne() {
        assertEquals(-1, globalAuthFilter.getOrder());
    }
}