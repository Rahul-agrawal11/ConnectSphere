package com.connectsphere.search.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * PostHashtag — many-to-many join between posts and hashtags.
 *
 * One row = one hashtag used in one post.
 * e.g. Post 5 contains #java and #springboot → two PostHashtag rows.
 *
 * postId references cs_post_db.posts.id (no FK — cross-service).
 * hashtagId references cs_search_db.hashtags.id (same DB — FK safe, but
 * we skip it to keep the entity simple and consistent with the pattern).
 *
 * The unique constraint prevents duplicate tag-post associations
 * when a post is re-indexed after an edit.
 */
@Entity
@Table(
        name = "post_hashtags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_post_hashtag",
                        columnNames = {"post_id", "hashtag_id"}
                )
        },
        indexes = {
                @Index(name = "idx_ph_post",    columnList = "post_id"),
                @Index(name = "idx_ph_hashtag", columnList = "hashtag_id"),
                @Index(name = "idx_ph_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References cs_post_db.posts.id — no FK (cross-service)
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // References cs_search_db.hashtags.id
    @Column(name = "hashtag_id", nullable = false)
    private Long hashtagId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}