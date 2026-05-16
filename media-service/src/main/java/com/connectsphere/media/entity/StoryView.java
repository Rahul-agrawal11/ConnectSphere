package com.connectsphere.media.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * StoryView entity — records each unique user who viewed a story.
 *
 * The composite unique constraint (story_id, viewer_id) ensures each user
 * is counted only once per story, regardless of how many times they open it.
 *
 * The viewsCount on Story is kept in sync atomically via StoryRepository.incrementViewsCount()
 * and is the fast read path. This table is the detailed audit trail for
 * the "Seen by" list shown to the story author.
 */
@Entity
@Table(
        name = "story_views",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_story_viewer",
                        columnNames = {"story_id", "viewer_id"}
                )
        },
        indexes = {
                @Index(name = "idx_story_view_story",  columnList = "story_id"),
                @Index(name = "idx_story_view_viewer", columnList = "viewer_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The story that was viewed */
    @Column(name = "story_id", nullable = false)
    private Long storyId;

    /**
     * The user who viewed the story.
     * References auth-service users.id — no FK (cross-service boundary).
     */
    @Column(name = "viewer_id", nullable = false)
    private Long viewerId;

    /** When the first view occurred */
    @CreationTimestamp
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private LocalDateTime viewedAt;
}