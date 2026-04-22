package com.connectsphere.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Comment entity supporting two-level threading.
 *
 * Threading model:
 *   parentCommentId = null  → top-level comment on a post
 *   parentCommentId = X     → reply to comment X
 *
 * We enforce exactly two levels in the service layer:
 *   you cannot reply to a reply.
 *
 * likesCount is a denormalised counter maintained by likeComment()
 * and unlikeComment() — avoids COUNT(*) on every comment render.
 *
 * isDeleted = true → soft-deleted; content replaced with
 * "[deleted]" in response so thread structure is preserved.
 */
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comment_post",   columnList = "post_id"),
                @Index(name = "idx_comment_author", columnList = "author_id"),
                @Index(name = "idx_comment_parent", columnList = "parent_comment_id"),
                @Index(name = "idx_comment_deleted",columnList = "is_deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References cs_post_db.posts.id — no FK (cross-service)
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // References cs_auth_db.users.id — no FK (cross-service)
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /**
     * null  → this is a top-level comment
     * non-null → this is a reply to the comment with this ID
     */
    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}