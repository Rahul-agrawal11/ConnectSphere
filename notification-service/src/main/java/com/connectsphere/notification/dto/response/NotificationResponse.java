package com.connectsphere.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notification data returned to clients.
 * Never exposes internal DB fields directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long recipientId;
    private Long actorId;
    private String type;
    private String message;
    private Long targetId;
    private String targetType;
    private String deepLinkUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;
}