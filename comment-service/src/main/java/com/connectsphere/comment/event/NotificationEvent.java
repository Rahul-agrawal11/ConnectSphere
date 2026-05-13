package com.connectsphere.comment.event;

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
    private Long recipientId;   // post author or parent-comment author
    private Long actorId;       // user who commented
    private String type;          // "COMMENT" or "REPLY"
    private String message;
    private Long targetId;      // postId or parentCommentId
    private String targetType;    // "POST" or "COMMENT"
    private String deepLinkUrl;
}