package com.connectsphere.follow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Follower and following counts for a user.
 * Used by profile pages to display social graph stats.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowCountResponse {

    private Long userId;
    private Long followerCount;
    private Long followingCount;
}