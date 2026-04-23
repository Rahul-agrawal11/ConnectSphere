package com.connectsphere.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Story data returned to clients.
 * Includes remaining time until expiry for frontend countdown display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryResponse {

    private Long id;
    private Long authorId;
    private String mediaUrl;
    private String caption;
    private String mediaType;
    private Integer viewsCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    // Computed field: seconds until expiry (0 if already expired)
    private Long secondsUntilExpiry;
}