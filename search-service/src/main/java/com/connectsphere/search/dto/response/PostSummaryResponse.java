package com.connectsphere.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight post summary for search results.
 *
 * This DTO is populated by calling post-service via Feign.
 * Only the fields needed for search result rendering are included —
 * not the full PostResponse with all counters.
 *
 * For Elasticsearch migration: this DTO maps directly to
 * the Elasticsearch document fields — no structural change needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSummaryResponse {

    private Long id;
    private Long authorId;
    private String content;
    private String visibility;
    private Integer likesCount;
    private Integer commentsCount;
    private LocalDateTime createdAt;
}