package com.connectsphere.like.service;

import com.connectsphere.like.dto.request.ReactRequest;
import com.connectsphere.like.dto.response.LikeResponse;
import com.connectsphere.like.dto.response.ReactionSummaryResponse;
import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Like service contract.
 */
public interface LikeService {

    // Core reaction operations
    LikeResponse react(Long userId, ReactRequest request);
    void unreact(Long userId, Long targetId, TargetType targetType);
    LikeResponse changeReaction(Long userId, Long targetId,
                                TargetType targetType, ReactionType newReactionType);

    // Query operations
    boolean hasReacted(Long userId, Long targetId, TargetType targetType);
    LikeResponse getUserReaction(Long userId, Long targetId, TargetType targetType);
    Page<LikeResponse> getReactionsByTarget(Long targetId, TargetType targetType,
                                            Pageable pageable);
    Page<LikeResponse> getReactionsByUser(Long userId, Pageable pageable);

    // Count operations
    long getReactionCount(Long targetId, TargetType targetType);
    long getReactionCountByType(Long targetId, TargetType targetType,
                                ReactionType reactionType);

    // Summary for emoji bar rendering
    ReactionSummaryResponse getReactionSummary(Long targetId, TargetType targetType);
}