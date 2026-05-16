package com.connectsphere.post.entity;

import com.connectsphere.post.enums.PostType;
import com.connectsphere.post.enums.PostVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core post entity.
 *
 * mediaUrls is stored as a comma-separated string in a TEXT column.
 * This avoids a join table for a simple list of URLs and keeps queries fast.
 *
 * likesCount, commentsCount, sharesCount are denormalised counters
 * updated via increment/decrement calls from like-service and comment-service.
 * This avoids expensive COUNT(*) queries on every feed render.
 *
 * isDeleted = true → soft-deleted; excluded from all user-facing queries
 * but retained for moderation audit (30-day retention per NFR).
 */
@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_post_author", columnList = "author_id"),
                @Index(name = "idx_post_visibility", columnList = "visibility"),
                @Index(name = "idx_post_created", columnList = "created_at"),
                @Index(name = "idx_post_deleted", columnList = "is_deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References cs_auth_db.users.id — no FK constraint (cross-service)
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(columnDefinition = "TEXT")
    private String content;

    // Comma-separated media URLs; empty string when no media
    @Column(name = "media_urls", columnDefinition = "TEXT")
    private String mediaUrls;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 10)
    @Builder.Default
    private PostType postType = PostType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private Integer commentsCount = 0;

    @Column(name = "shares_count", nullable = false)
    @Builder.Default
    private Integer sharesCount = 0;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Convenience helpers ─────────────────────────────────────────────

    /**
     * Parse comma-separated mediaUrls into a List.
     * Returns empty list when no media is attached.
     */
    @Transient
    public List<String> getMediaUrlList() {
        if (mediaUrls == null || mediaUrls.isBlank()) {
            return new ArrayList<>();
        }
        return List.of(mediaUrls.split(","));
    }

    /**
     * Set media URL list by joining with commas.
     */
    public void setMediaUrlList(List<String> urls) {
        this.mediaUrls = (urls == null || urls.isEmpty())
                ? null
                : String.join(",", urls);
        this.postType = (urls == null || urls.isEmpty())
                ? PostType.TEXT
                : PostType.MEDIA;
    }
}