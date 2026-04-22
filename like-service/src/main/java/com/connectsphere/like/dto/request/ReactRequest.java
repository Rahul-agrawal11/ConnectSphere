package com.connectsphere.like.dto.request;

import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for POST /api/v1/likes (react to a target).
 *
 * Examples:
 *   React LOVE to post 5:
 *     { "targetId": 5, "targetType": "POST", "reactionType": "LOVE" }
 *
 *   React HAHA to comment 12:
 *     { "targetId": 12, "targetType": "COMMENT", "reactionType": "HAHA" }
 */
@Data
public class ReactRequest {

    @NotNull(message = "targetId is required")
    private Long targetId;

    @NotNull(message = "targetType is required (POST or COMMENT)")
    private TargetType targetType;

    @NotNull(message = "reactionType is required")
    private ReactionType reactionType;
}