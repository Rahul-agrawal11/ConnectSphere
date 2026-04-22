package com.connectsphere.like.entity;

import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Reaction entity — one row per user per target.
 *
 * The unique constraint on (user_id, target_id, target_type) enforces
 * the business rule: one reaction per user per target at the DB level.
 * This prevents race conditions even under concurrent requests.
 *
 * targetType distinguishes posts from comments so targetId alone is
 * not globally unique (post #1 and comment #1 are different targets).
 *
 * reactionType can change (LIKE → LOVE) via changeReaction() without
 * deleting and re-inserting — this avoids double-counting the counters.
 */
@Entity
@Table(
        name = "likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_target",
                        columnNames = {"user_id", "target_id", "target_type"}
                )
        },
        indexes = {
                @Index(name = "idx_like_target",
                        columnList = "target_id, target_type"),
                @Index(name = "idx_like_user",
                        columnList = "user_id"),
                @Index(name = "idx_like_user_target",
                        columnList = "user_id, target_id, target_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References cs_auth_db.users.id — no FK (cross-service)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // References cs_post_db.posts.id OR cs_comment_db.comments.id
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 10)
    private ReactionType reactionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}