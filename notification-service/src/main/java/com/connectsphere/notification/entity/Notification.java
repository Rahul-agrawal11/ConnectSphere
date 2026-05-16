package com.connectsphere.notification.entity;

import com.connectsphere.notification.enums.NotificationTargetType;
import com.connectsphere.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification entity.
 *
 * Each notification represents a single social event alert.
 *
 * recipientId → who receives this notification
 * actorId     → who triggered the event (null for BROADCAST/SYSTEM)
 * type        → the event type (LIKE, COMMENT, FOLLOW etc.)
 * targetId    → the entity ID the notification links to
 * targetType  → what kind of entity targetId refers to
 * message     → human-readable notification text
 * deepLinkUrl → pre-computed URL for frontend navigation
 * isRead      → false until the user opens/reads it
 *
 * No updatedAt — notifications are either unread or read (one-way transition).
 * No soft delete — notifications are hard-deleted when dismissed.
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notif_recipient",
                        columnList = "recipient_id"),
                @Index(name = "idx_notif_recipient_read",
                        columnList = "recipient_id, is_read"),
                @Index(name = "idx_notif_type",
                        columnList = "type"),
                @Index(name = "idx_notif_created",
                        columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who will see this notification
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    // The user who triggered the event (null for system/broadcast)
    @Column(name = "actor_id")
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    // Human-readable notification text
    // e.g. "John liked your post", "Sarah started following you"
    @Column(nullable = false)
    private String message;

    // The entity this notification is about (postId, commentId, userId)
    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 10)
    private NotificationTargetType targetType;

    // Pre-computed deep link URL for frontend navigation
    @Column(name = "deep_link_url")
    private String deepLinkUrl;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}