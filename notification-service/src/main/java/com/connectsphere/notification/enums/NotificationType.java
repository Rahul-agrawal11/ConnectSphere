package com.connectsphere.notification.enums;

/**
 * Types of notifications supported by ConnectSphere.
 *
 * LIKE      — someone reacted to your post or comment
 * COMMENT   — someone commented on your post
 * REPLY     — someone replied to your comment
 * FOLLOW    — someone started following you
 * MENTION   — someone mentioned you in a post or comment
 * BROADCAST — admin platform-wide announcement
 */
public enum NotificationType {
    LIKE,
    COMMENT,
    REPLY,
    FOLLOW,
    MENTION,
    BROADCAST
}