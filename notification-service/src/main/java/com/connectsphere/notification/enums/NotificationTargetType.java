package com.connectsphere.notification.enums;

/**
 * The type of entity this notification deep-links to.
 * Used by the frontend to construct the correct navigation URL.
 *
 * POST    → /posts/{targetId}
 * COMMENT → /posts/{postId}#comment-{targetId}
 * USER    → /profile/{targetId}
 * STORY   → /stories/{targetId}
 * MEDIA   → /media/{targetId}
 * SYSTEM  → no deep link (broadcast / system alerts)
 */
public enum NotificationTargetType {
    POST,
    COMMENT,
    USER,
    STORY,
    MEDIA,
    SYSTEM
}