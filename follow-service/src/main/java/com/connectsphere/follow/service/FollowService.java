package com.connectsphere.follow.service;

import com.connectsphere.follow.dto.response.FollowCountResponse;
import com.connectsphere.follow.dto.response.FollowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Follow service contract.
 */
public interface FollowService {

    // Core follow operations
    FollowResponse follow(Long followerId, Long followeeId);
    void unfollow(Long followerId, Long followeeId);
    boolean isFollowing(Long followerId, Long followeeId);

    // Social graph queries
    Page<FollowResponse> getFollowers(Long userId, Pageable pageable);
    Page<FollowResponse> getFollowing(Long userId, Pageable pageable);

    // Raw ID lists — used internally and by other services
    List<Long> getFollowingIds(Long userId);
    List<Long> getFollowerIds(Long userId);

    // Counts
    FollowCountResponse getFollowCounts(Long userId);
    long getFollowerCount(Long userId);
    long getFollowingCount(Long userId);

    // Graph analysis
    List<Long> getMutualFollowIds(Long userId);
    List<Long> getSuggestedUserIds(Long userId, int limit);
}