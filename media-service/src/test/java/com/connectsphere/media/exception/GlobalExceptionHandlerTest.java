package com.connectsphere.media.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/media/1");
    }

    @Test
    void handleMediaNotFound_shouldReturn404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMediaNotFound(new MediaNotFoundException("Media not found: 1"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("message", "Media not found: 1");
    }

    @Test
    void handleStoryNotFound_shouldReturn404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleStoryNotFound(new StoryNotFoundException("Story not found: 99"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
    }

    @Test
    void handleUnauthorized_shouldReturn403() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnauthorized(new UnauthorizedActionException("Not allowed"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("status", 403);
        assertThat(response.getBody()).containsEntry("message", "Not allowed");
    }

    @Test
    void handleIllegalArgument_shouldReturn400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("File type not allowed"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "File type not allowed");
    }

    @Test
    void handleMaxUploadSize_shouldReturn413() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMaxUploadSize(new MaxUploadSizeExceededException(10L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsEntry("status", 413);
        assertThat(response.getBody()).containsEntry("message", "File size exceeds the maximum allowed limit.");
    }

    @Test
    void handleStorageException_shouldReturn500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleStorageException(new StorageException("Disk full"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("message", "Disk full");
    }

    @Test
    void handleGeneral_shouldReturn500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneral(new RuntimeException("Unexpected"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
    }

    @Test
    void responseBody_shouldContainRequiredFields() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMediaNotFound(new MediaNotFoundException("x"), request);

        Map<String, Object> body = response.getBody();
        assertThat(body).containsKeys("timestamp", "status", "error", "message", "path");
        assertThat(body.get("path")).isEqualTo("/api/v1/media/1");
    }

    @Test
    void storageException_withCause_shouldWrapCause() {
        Throwable cause = new RuntimeException("disk error");
        StorageException ex = new StorageException("Failed to store", cause);

        assertThat(ex.getMessage()).isEqualTo("Failed to store");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}