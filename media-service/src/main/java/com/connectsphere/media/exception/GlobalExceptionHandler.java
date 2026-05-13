package com.connectsphere.media.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for media-service.
 *
 * FIX: Added MissingRequestHeaderException handler.
 * Previously, missing required headers (e.g. X-User-Id on auth-required
 * endpoints) fell through to handleGeneral → 500 Internal Server Error.
 * Now returns 400 Bad Request with a clear message.
 *
 * This provides a safety net even though the primary fix is making
 * X-User-Id optional (required=false) on public StoryController endpoints.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation ────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST,
                "Validation failed", request.getRequestURI());
        body.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── Missing Header ────────────────────────────────────────────────────

    /**
     * FIX: Returns 400 instead of 500 when a required @RequestHeader is absent.
     *
     * Example: POST /api/v1/stories without an Authorization JWT (X-User-Id missing)
     * now returns: 400 Bad Request — "Required header 'X-User-Id' is missing"
     * instead of: 500 Internal Server Error (caught by handleGeneral).
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {

        log.warn("Missing required header '{}' for path: {}",
                ex.getHeaderName(), request.getRequestURI());
        return ResponseEntity.badRequest().body(
                buildBody(HttpStatus.BAD_REQUEST,
                        "Required header '" + ex.getHeaderName() + "' is missing — " +
                                "this endpoint requires authentication.",
                        request.getRequestURI()));
    }

    // ── Missing Request Parameter ─────────────────────────────────────────

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        return ResponseEntity.badRequest().body(
                buildBody(HttpStatus.BAD_REQUEST,
                        "Required parameter '" + ex.getParameterName() + "' is missing.",
                        request.getRequestURI()));
    }

    // ── Domain Exceptions ─────────────────────────────────────────────────

    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMediaNotFound(
            MediaNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildBody(HttpStatus.NOT_FOUND, ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(StoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStoryNotFound(
            StoryNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildBody(HttpStatus.NOT_FOUND, ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedActionException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildBody(HttpStatus.FORBIDDEN, ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                buildBody(HttpStatus.BAD_REQUEST, ex.getMessage(),
                        request.getRequestURI()));
    }

    // ── File Size Limit ───────────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                buildBody(HttpStatus.PAYLOAD_TOO_LARGE,
                        "File size exceeds the maximum allowed limit.",
                        request.getRequestURI()));
    }

    // ── Catch-All ────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(),
                ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                buildBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred",
                        request.getRequestURI()));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Map<String, Object> buildBody(HttpStatus status, String message,
                                          String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        body.put("path",      path);
        return body;
    }
}