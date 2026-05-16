package com.connectsphere.notification.dto.request;

import com.connectsphere.notification.enums.NotificationTargetType;
import com.connectsphere.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for POST /api/v1/notifications/bulk
 *
 * Used by admins to broadcast a notification to multiple users
 * or to all platform users.
 *
 * If recipientIds is empty and broadcastToAll is true,
 * the service sends to all active users (requires auth-service call).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequest {

    // Specific recipient IDs — leave empty if broadcastToAll = true
    @NotEmpty(message = "recipientIds must not be empty")
    private List<Long> recipientIds;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotBlank(message = "message is required")
    private String message;

    private Long targetId;

    private NotificationTargetType targetType;

    private String deepLinkUrl;
}