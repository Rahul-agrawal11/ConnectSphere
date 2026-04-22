package com.connectsphere.like.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Aggregated reaction summary for a target.
 *
 * Used by the frontend to render the emoji reaction bar.
 *
 * Example response:
 * {
 *   "targetId": 5,
 *   "targetType": "POST",
 *   "totalCount": 18,
 *   "reactions": {
 *     "LIKE": 10,
 *     "LOVE": 5,
 *     "HAHA": 2,
 *     "WOW": 1
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummaryResponse {

    private Long targetId;
    private String targetType;
    private Long totalCount;

    // Map of reactionType name → count
    // Only includes types with at least one reaction (sparse map)
    private Map<String, Long> reactions;
}