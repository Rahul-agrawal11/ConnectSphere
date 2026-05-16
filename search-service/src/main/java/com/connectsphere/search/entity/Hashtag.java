package com.connectsphere.search.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Hashtag entity — one row per unique tag across the platform.
 *
 * tag is stored in lowercase, normalised form.
 * e.g. "#SpringBoot" and "#springboot" both map to "springboot".
 *
 * postCount is a denormalised counter maintained by indexPost()
 * and removePostIndex(). Avoids COUNT(*) on PostHashtag for trending.
 *
 * lastUsedAt is updated every time a post using this tag is indexed.
 * Used to filter out stale tags from trending results.
 *
 * The unique constraint on 'tag' enforces exactly one row per tag word.
 * Upsert pattern: findByTag → update if exists, insert if not.
 */
@Entity
@Table(
        name = "hashtags",
        indexes = {
                @Index(name = "idx_hashtag_tag",
                        columnList = "tag"),
                @Index(name = "idx_hashtag_count",
                        columnList = "post_count DESC"),
                @Index(name = "idx_hashtag_last_used",
                        columnList = "last_used_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String tag;

    @Column(name = "post_count", nullable = false)
    @Builder.Default
    private Integer postCount = 0;

    @UpdateTimestamp
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}