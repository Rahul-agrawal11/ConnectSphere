package com.connectsphere.notification.event;

import com.connectsphere.notification.enums.NotificationTargetType;
import com.connectsphere.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RabbitMQ event message for notification dispatch.
 *
 * Published by any service that triggers a social event.
 * Deserialized from JSON by the NotificationEventListener.
 *
 * Implements Serializable as a safety measure for message serialization,
 * though Jackson JSON serialization is the primary mechanism.
 *
 * Message flow:
 *   like-service / comment-service / follow-service
 *     → publish NotificationEvent to connectsphere.events exchange
 *     → routing key: notification.like / notification.comment / etc.
 *     → connectsphere.notification.queue
 *     → NotificationEventListener.handleNotificationEvent()
 *     → NotificationService.createNotification()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private Long recipientId;
    private Long actorId;
    private NotificationType type;
    private String message;
    private Long targetId;
    private NotificationTargetType targetType;
    private String deepLinkUrl;
}