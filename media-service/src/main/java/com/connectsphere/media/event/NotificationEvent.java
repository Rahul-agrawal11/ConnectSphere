package com.connectsphere.media.event;

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
    private Long recipientId;   // a follower who receives the notification
    private Long actorId;       // user who uploaded the story
    private String type;          // "STORY"  (not in NotificationType yet — add it below)
    private String message;
    private Long targetId;      // storyId
    private String targetType;    // "STORY"
    private String deepLinkUrl;
}