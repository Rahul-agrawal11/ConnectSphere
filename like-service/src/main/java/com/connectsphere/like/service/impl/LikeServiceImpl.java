package com.connectsphere.like.service.impl;

import com.connectsphere.like.client.CommentServiceClient;
import com.connectsphere.like.client.PostServiceClient;
import com.connectsphere.like.dto.request.ReactRequest;
import com.connectsphere.like.dto.response.LikeResponse;
import com.connectsphere.like.dto.response.ReactionSummaryResponse;
import com.connectsphere.like.entity.Like;
import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import com.connectsphere.like.exception.DuplicateReactionException;
import com.connectsphere.like.exception.LikeNotFoundException;
import com.connectsphere.like.repository.LikeRepository;
import com.connectsphere.like.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PostServiceClient postServiceClient;
    private final CommentServiceClient commentServiceClient;

    // ── Core Reaction Operations ─────────────────────────────────────────

    @Override
    @Transactional
    public LikeResponse react(Long userId, ReactRequest request) {

        // Enforce one reaction per user per target
        if (likeRepository.existsByUserIdAndTargetIdAndTargetType(
                userId, request.getTargetId(), request.getTargetType())) {
            throw new DuplicateReactionException(
                    "You have already reacted to this " +
                            request.getTargetType().name().toLowerCase() +
                            ". Use change-reaction to update your reaction.");
        }

        Like like = Like.builder()
                .userId(userId)
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .reactionType(request.getReactionType())
                .build();

        Like saved = likeRepository.save(like);

        // Notify the target service to increment its likesCount counter
        incrementTargetCounter(request.getTargetId(), request.getTargetType());

        log.info("Reaction saved: userId={} reacted {} to {}:{}",
                userId, request.getReactionType(),
                request.getTargetType(), request.getTargetId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void unreact(Long userId, Long targetId, TargetType targetType) {

        Like existing = likeRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
                .orElseThrow(() -> new LikeNotFoundException(
                        "No reaction found for this user on " +
                                targetType.name().toLowerCase() + " " + targetId));

        likeRepository.deleteByUserIdAndTargetIdAndTargetType(
                userId, targetId, targetType);

        // Notify the target service to decrement its likesCount counter
        decrementTargetCounter(targetId, targetType);

        log.info("Reaction removed: userId={} unreacted from {}:{}",
                userId, targetType, targetId);
    }

    @Override
    @Transactional
    public LikeResponse changeReaction(Long userId, Long targetId,
                                       TargetType targetType,
                                       ReactionType newReactionType) {

        Like existing = likeRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
                .orElseThrow(() -> new LikeNotFoundException(
                        "No existing reaction found. React first before changing."));

        ReactionType oldType = existing.getReactionType();

        if (oldType == newReactionType) {
            // No change needed — return current reaction as-is
            log.debug("Reaction type unchanged for userId={} on {}:{}",
                    userId, targetType, targetId);
            return mapToResponse(existing);
        }

        // Update in-place — no counter change needed since total count stays the same
        existing.setReactionType(newReactionType);
        Like updated = likeRepository.save(existing);

        log.info("Reaction changed: userId={} changed {} → {} on {}:{}",
                userId, oldType, newReactionType, targetType, targetId);

        return mapToResponse(updated);
    }

    // ── Query Operations ─────────────────────────────────────────────────

    @Override
    public boolean hasReacted(Long userId, Long targetId, TargetType targetType) {
        return likeRepository.existsByUserIdAndTargetIdAndTargetType(
                userId, targetId, targetType);
    }

    /**
     * Returns the current user's reaction on a target, or null if none exists.
     *
     * FIX: Previously threw LikeNotFoundException (404) when no reaction existed.
     * This caused console errors in the frontend for every post load.
     * Now returns null data with a 200 OK so the frontend can treat it as "no reaction".
     */
    @Override
    public LikeResponse getUserReaction(Long userId, Long targetId,
                                        TargetType targetType) {
        Optional<Like> like = likeRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
        // Return null if not found — controller will wrap in ApiResponse with null data
        return like.map(this::mapToResponse).orElse(null);
    }

    @Override
    public Page<LikeResponse> getReactionsByTarget(Long targetId,
                                                   TargetType targetType,
                                                   Pageable pageable) {
        return likeRepository
                .findByTargetIdAndTargetTypeOrderByCreatedAtDesc(
                        targetId, targetType, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<LikeResponse> getReactionsByUser(Long userId, Pageable pageable) {
        return likeRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    // ── Count Operations ─────────────────────────────────────────────────

    @Override
    public long getReactionCount(Long targetId, TargetType targetType) {
        return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
    }

    @Override
    public long getReactionCountByType(Long targetId, TargetType targetType,
                                       ReactionType reactionType) {
        return likeRepository.countByTargetIdAndTargetTypeAndReactionType(
                targetId, targetType, reactionType);
    }

    // ── Reaction Summary ─────────────────────────────────────────────────

    @Override
    public ReactionSummaryResponse getReactionSummary(Long targetId,
                                                      TargetType targetType) {
        List<Object[]> rawSummary = likeRepository
                .getReactionSummaryRaw(targetId, targetType);

        // Build the reaction map from the grouped query results
        Map<String, Long> reactionMap = new HashMap<>();
        long totalCount = 0L;

        for (Object[] row : rawSummary) {
            ReactionType type = (ReactionType) row[0];
            Long count = (Long) row[1];
            reactionMap.put(type.name(), count);
            totalCount += count;
        }

        return ReactionSummaryResponse.builder()
                .targetId(targetId)
                .targetType(targetType.name())
                .totalCount(totalCount)
                .reactions(reactionMap)
                .build();
    }

    // ── Private Helpers ──────────────────────────────────────────────────

    /**
     * Route the counter increment to the correct downstream service
     * based on the targetType.
     * Failures are logged but do not roll back the reaction save.
     * STORY targets have no counter service — skip silently.
     */
    private void incrementTargetCounter(Long targetId, TargetType targetType) {
        try {
            switch (targetType) {
                case POST    -> postServiceClient.incrementLikesCount(targetId);
                case COMMENT -> commentServiceClient.incrementLikesCount(targetId);
                case STORY   -> log.debug("Story reactions do not increment a counter service");
            }
        } catch (Exception e) {
            log.warn("Failed to increment likesCount on {}:{} — {}",
                    targetType, targetId, e.getMessage());
        }
    }

    /**
     * Route the counter decrement to the correct downstream service.
     * Failures are logged but do not fail the unreact operation.
     * STORY targets have no counter service — skip silently.
     */
    private void decrementTargetCounter(Long targetId, TargetType targetType) {
        try {
            switch (targetType) {
                case POST    -> postServiceClient.decrementLikesCount(targetId);
                case COMMENT -> commentServiceClient.decrementLikesCount(targetId);
                case STORY   -> log.debug("Story reactions do not decrement a counter service");
            }
        } catch (Exception e) {
            log.warn("Failed to decrement likesCount on {}:{} — {}",
                    targetType, targetId, e.getMessage());
        }
    }

    /**
     * Map entity to response DTO.
     */
    private LikeResponse mapToResponse(Like like) {
        return LikeResponse.builder()
                .id(like.getId())
                .userId(like.getUserId())
                .targetId(like.getTargetId())
                .targetType(like.getTargetType().name())
                .reactionType(like.getReactionType().name())
                .createdAt(like.getCreatedAt())
                .updatedAt(like.getUpdatedAt())
                .build();
    }
}