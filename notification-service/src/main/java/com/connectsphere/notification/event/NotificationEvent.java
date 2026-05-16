package com.connectsphere.notification.event;

import com.connectsphere.notification.enums.NotificationTargetType;
import com.connectsphere.notification.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * Message flow:
 *   like-service / comment-service / follow-service / media-service
 *     → publish NotificationEvent to connectsphere.events exchange
 *     → routing key: notification.like / notification.comment / notification.story / etc.
 *     → connectsphere.notification.queue
 *     → NotificationEventListener.handleNotificationEvent()
 *     → NotificationService.createNotification()
 *
 * NOTE: @JsonIgnoreProperties(ignoreUnknown = true) prevents deserialization failures
 * if publisher services add new fields in the future.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationEvent implements Serializable {

    private Long recipientId;
    private Long actorId;
    private NotificationType type;
    private String message;
    private Long targetId;
    private NotificationTargetType targetType;
    private String deepLinkUrl;
}