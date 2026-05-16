package com.connectsphere.notification.dto.request;

import com.connectsphere.notification.enums.NotificationTargetType;
import com.connectsphere.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/v1/notifications
 *
 * Called directly by other microservices (like-service, comment-service,
 * follow-service) when a social event occurs.
 *
 * Example — follow-service calls this after a follow is created:
 * {
 *   "recipientId": 5,
 *   "actorId": 2,
 *   "type": "FOLLOW",
 *   "message": "johndoe started following you",
 *   "targetId": 2,
 *   "targetType": "USER",
 *   "deepLinkUrl": "/profile/2"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    @NotNull(message = "recipientId is required")
    private Long recipientId;

    // Nullable for BROADCAST / SYSTEM notifications
    private Long actorId;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotBlank(message = "message is required")
    private String message;

    private Long targetId;

    private NotificationTargetType targetType;

    private String deepLinkUrl;
}