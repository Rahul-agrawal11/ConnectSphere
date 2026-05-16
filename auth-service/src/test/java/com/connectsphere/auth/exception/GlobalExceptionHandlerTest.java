package com.connectsphere.auth.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
    }

    @Test
    void handleUserAlreadyExists_shouldReturn409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUserAlreadyExists(
                        new UserAlreadyExistsException("Email already registered"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("message", "Email already registered");
    }

    @Test
    void handleUserNotFound_shouldReturn404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUserNotFound(
                        new UserNotFoundException("User not found with id: 99"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("message", "User not found with id: 99");
    }

    @Test
    void handleInvalidCredentials_shouldReturn401() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidCredentials(
                        new InvalidCredentialsException("Invalid email/username or password"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("status", 401);
    }

    @Test
    void handleInvalidToken_shouldReturn401() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidToken(
                        new InvalidTokenException("Refresh token not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("message", "Refresh token not found");
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDenied(
                        new AccessDeniedException("forbidden"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("status", 403);
        assertThat(response.getBody()).containsEntry("message",
                "You do not have permission to perform this action");
    }

    @Test
    void handleGeneral_shouldReturn500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneral(new RuntimeException("Something broke"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
    }

    @Test
    void responseBody_shouldContainAllRequiredFields() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUserNotFound(new UserNotFoundException("x"), request);

        assertThat(response.getBody()).containsKeys("timestamp", "status", "error", "message", "path");
        assertThat(response.getBody().get("path")).isEqualTo("/api/v1/auth/login");
    }
}