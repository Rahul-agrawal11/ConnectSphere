package com.connectsphere.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Hashtag data returned to clients.
 * Includes postCount for trending bar and feed display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagResponse {

    private Long id;
    private String tag;
    private Integer postCount;
    private LocalDateTime lastUsedAt;
}