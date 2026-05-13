package com.connectsphere.like.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for like-service.
 *
 * FIX 1: Added MissingRequestHeaderException — returns 400 instead of 500
 *   when a required @RequestHeader (e.g. X-User-Id) is absent.
 *
 * FIX 2: Added MissingServletRequestParameterException — returns 400 instead
 *   of 500 when required @RequestParam values (targetId, targetType) are absent.
 *
 * FIX 3: Added MethodArgumentTypeMismatchException — returns 400 with a clear
 *   message when an enum value like TargetType or ReactionType is invalid
 *   (e.g. passing 'STORY' before the enum was updated, or a typo).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Bean Validation ───────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        Map<String, Object> body = build(HttpStatus.BAD_REQUEST,
                "Validation failed", request.getRequestURI(), null);
        body.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── Missing Request Header ────────────────────────────────────────────

    /**
     * FIX: Returns 400 instead of 500 when X-User-Id header is absent
     * on a write endpoint (react, unreact, change-reaction).
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {

        log.warn("Missing required header '{}' on {}: ",
                ex.getHeaderName(), request.getRequestURI());
        return ResponseEntity.badRequest().body(
                build(HttpStatus.BAD_REQUEST,
                        "Required header '" + ex.getHeaderName() +
                                "' is missing. This endpoint requires authentication.",
                        request.getRequestURI(), null));
    }

    // ── Missing Request Parameter ─────────────────────────────────────────

    /**
     * FIX: Returns 400 instead of 500 when targetId, targetType, or
     * reactionType query params are missing.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        return ResponseEntity.badRequest().body(
                build(HttpStatus.BAD_REQUEST,
                        "Required parameter '" + ex.getParameterName() + "' is missing.",
                        request.getRequestURI(), null));
    }

    // ── Enum / Type Mismatch ──────────────────────────────────────────────

    /**
     * FIX: Returns 400 with a clear message when targetType or reactionType
     * is not a valid enum value (e.g. passing "STORY" before enum update,
     * or a casing error like "like" instead of "LIKE").
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message;
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            message = "Invalid value '" + ex.getValue() +
                    "' for parameter '" + ex.getName() +
                    "'. Allowed values: " + java.util.Arrays.toString(
                    ex.getRequiredType().getEnumConstants());
        } else {
            message = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();
        }
        return ResponseEntity.badRequest().body(
                build(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null));
    }

    // ── Domain Exceptions ─────────────────────────────────────────────────

    @ExceptionHandler(DuplicateReactionException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateReactionException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                build(HttpStatus.CONFLICT, ex.getMessage(),
                        request.getRequestURI(), null));
    }

    @ExceptionHandler(LikeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            LikeNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                build(HttpStatus.NOT_FOUND, ex.getMessage(),
                        request.getRequestURI(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                build(HttpStatus.BAD_REQUEST, ex.getMessage(),
                        request.getRequestURI(), null));
    }

    // ── Catch-All ────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(),
                ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                build(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred",
                        request.getRequestURI(), null));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Map<String, Object> build(HttpStatus status, String message,
                                      String path, Object errors) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        body.put("path",      path);
        if (errors != null) body.put("errors", errors);
        return body;
    }
}