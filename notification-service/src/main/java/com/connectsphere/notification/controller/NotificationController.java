package com.connectsphere.notification.controller;

import com.connectsphere.notification.dto.request.BulkNotificationRequest;
import com.connectsphere.notification.dto.request.CreateNotificationRequest;
import com.connectsphere.notification.dto.response.ApiResponse;
import com.connectsphere.notification.dto.response.NotificationResponse;
import com.connectsphere.notification.enums.NotificationType;
import com.connectsphere.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Notification REST controller.
 *
 * Recipient-scoped endpoints use X-User-Id to scope results
 * to the authenticated user's own notifications.
 *
 * Internal create endpoint is called by other services (no X-User-Id needed).
 * Admin bulk endpoint checks X-User-Role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications",
        description = "In-app notification management — read, mark, delete, bulk")
public class NotificationController {

    private final NotificationService notificationService;

    // ── Internal Create (called by other services) ────────────────────────

    @Operation(
            summary = "[Internal] Create a notification",
            description = "Called directly by like-service, comment-service, " +
                    "and follow-service when social events occur. " +
                    "Not intended for direct client use."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {

        NotificationResponse response =
                notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification created", response));
    }

    // ── Admin Bulk Broadcast ──────────────────────────────────────────────

    @Operation(
            summary = "[ADMIN] Send bulk notification to multiple users",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> sendBulkNotification(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody BulkNotificationRequest request) {

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }
        notificationService.sendBulkNotification(request);
        return ResponseEntity.ok(
                ApiResponse.success("Bulk notification dispatched to " +
                        request.getRecipientIds().size() + " users"));
    }

    // ── Get All Notifications ─────────────────────────────────────────────

    @Operation(
            summary = "Get all notifications for the current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Notifications fetched",
                        notificationService.getNotificationsByRecipient(
                                userId, pageable)));
    }

    // ── Get Unread Notifications ──────────────────────────────────────────

    @Operation(
            summary = "Get unread notifications for the current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUnread(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Unread notifications",
                        notificationService.getUnreadNotifications(
                                userId, pageable)));
    }

    // ── Get Notifications By Type ─────────────────────────────────────────

    @Operation(
            summary = "Get notifications filtered by type",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getByType(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Notifications by type",
                        notificationService.getNotificationsByType(
                                userId, type, pageable)));
    }

    // ── Unread Count (Badge) ──────────────────────────────────────────────

    @Operation(
            summary = "Get unread notification count (badge value)",
            description = "Called on every page load to update the badge in the nav bar.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success("Unread count",
                        notificationService.getUnreadCount(userId)));
    }

    // ── Mark As Read ──────────────────────────────────────────────────────

    @Operation(
            summary = "Mark a single notification as read",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader("X-User-Id") Long userId) {

        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @Operation(
            summary = "Mark all notifications as read",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @RequestHeader("X-User-Id") Long userId) {

        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(
                ApiResponse.success(count + " notifications marked as read", count));
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @Operation(
            summary = "Delete a single notification",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long notificationId,
            @RequestHeader("X-User-Id") Long userId) {

        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted"));
    }

    @Operation(
            summary = "Delete all notifications for the current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<Void>> deleteAllNotifications(
            @RequestHeader("X-User-Id") Long userId) {

        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.ok(
                ApiResponse.success("All notifications deleted"));
    }
}