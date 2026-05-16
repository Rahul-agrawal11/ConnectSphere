package com.connectsphere.like.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Individual reaction record returned to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {

    private Long id;
    private Long userId;
    private Long targetId;
    private String targetType;
    private String reactionType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}