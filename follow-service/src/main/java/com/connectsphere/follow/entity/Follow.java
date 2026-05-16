package com.connectsphere.follow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Follow entity — one row per directed follow relationship.
 *
 * Graph model:
 *   followerId  → the user who pressed "Follow"
 *   followeeId  → the user being followed
 *
 * Example: User 1 follows User 2
 *   followerId = 1, followeeId = 2
 *
 * The unique constraint on (follower_id, followee_id) enforces:
 *   - No duplicate follows
 *   - Enforced at DB level to survive race conditions
 *
 * Self-follow prevention is enforced at the service layer with a
 * clear business exception before DB insertion.
 *
 * No updatedAt — follow records are immutable once created.
 * Unfollow = delete the row.
 */
@Entity
@Table(
        name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_follower_followee",
                        columnNames = {"follower_id", "followee_id"}
                )
        },
        indexes = {
                @Index(name = "idx_follow_follower",  columnList = "follower_id"),
                @Index(name = "idx_follow_followee",  columnList = "followee_id"),
                @Index(name = "idx_follow_both",
                        columnList = "follower_id, followee_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who is following
    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    // The user being followed
    @Column(name = "followee_id", nullable = false)
    private Long followeeId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}