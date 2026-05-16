package com.connectsphere.like.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    private Long recipientId;   // post/comment owner who receives the notification
    private Long actorId;       // user who liked
    private String type;          // "LIKE"
    private String message;
    private Long targetId;
    private String targetType;    // "POST" | "COMMENT" | "STORY"
    private String deepLinkUrl;
}