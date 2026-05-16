package com.connectsphere.follow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single follow relationship.
 * Returned when listing followers or following.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponse {

    private Long id;
    private Long followerId;
    private Long followeeId;
    private LocalDateTime createdAt;
}