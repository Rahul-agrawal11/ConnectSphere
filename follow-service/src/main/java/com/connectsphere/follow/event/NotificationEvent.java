package com.connectsphere.follow.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event published to RabbitMQ when a follow action occurs.
 * Consumed by notification-service via connectsphere.events exchange.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private Long recipientId;   // the user being followed (receives the notification)
    private Long actorId;       // the user who followed
    private String type;        // always "FOLLOW"
    private String message;
    private Long targetId;
    private String targetType;
    private String deepLinkUrl;
}