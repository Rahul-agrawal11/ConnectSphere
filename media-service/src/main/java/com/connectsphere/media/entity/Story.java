package com.connectsphere.media.entity;

import com.connectsphere.media.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Story entity — ephemeral media visible to followers for 24 hours.
 *
 * expiresAt is set to createdAt + 24 hours on save.
 * The StoryExpiryScheduler runs every 5 minutes and sets
 * isActive = false on stories where expiresAt < NOW().
 *
 * viewsCount is incremented atomically via a JPQL update query
 * to avoid optimistic locking conflicts under concurrent views.
 *
 * isActive = false means the story has expired or been manually deleted.
 * Stories are NOT hard-deleted immediately — kept for audit.
 */
@Entity
@Table(
        name = "stories",
        indexes = {
                @Index(name = "idx_story_author",   columnList = "author_id"),
                @Index(name = "idx_story_active",   columnList = "is_active"),
                @Index(name = "idx_story_expires",  columnList = "expires_at"),
                @Index(name = "idx_story_author_active",
                        columnList = "author_id, is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References cs_auth_db.users.id — no FK (cross-service)
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    // URL to the story media (image or short video)
    @Column(name = "media_url", nullable = false)
    private String mediaUrl;

    // Optional caption displayed on the story
    @Column(length = 500)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType;

    @Column(name = "views_count", nullable = false)
    @Builder.Default
    private Integer viewsCount = 0;

    // Computed at insert time: createdAt + 24 hours
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // false once expired (scheduler sets this) or manually deleted
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}