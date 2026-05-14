package com.connectsphere.like.service.impl;

import com.connectsphere.like.client.CommentServiceClient;
import com.connectsphere.like.client.PostServiceClient;
import com.connectsphere.like.dto.request.ReactRequest;
import com.connectsphere.like.dto.response.LikeResponse;
import com.connectsphere.like.dto.response.ReactionSummaryResponse;
import com.connectsphere.like.entity.Like;
import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import com.connectsphere.like.event.NotificationEvent;
import com.connectsphere.like.exception.DuplicateReactionException;
import com.connectsphere.like.exception.LikeNotFoundException;
import com.connectsphere.like.repository.LikeRepository;
import com.connectsphere.like.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-keys.like}")
    private String likeRoutingKey;

    // ── Core Reaction Operations ─────────────────────────────────────────

    @Override
    @Transactional
    public LikeResponse react(Long userId, ReactRequest request) {

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

        incrementTargetCounter(request.getTargetId(), request.getTargetType());

        // ── Publish like notification ──────────────────────────────────────
        publishLikeNotification(userId, request.getTargetId(), request.getTargetType());

        log.info("Reaction saved: userId={} reacted {} to {}:{}",
                userId, request.getReactionType(),
                request.getTargetType(), request.getTargetId());

        return mapToResponse(saved);
    }

    // ── Notification Publisher ────────────────────────────────────────────

    private void publishLikeNotification(Long actorId, Long targetId, TargetType targetType) {
        try {
            Long ownerId = resolveOwnerId(targetId, targetType);

            // Don't notify if the user liked their own content
            if (ownerId == null || ownerId.equals(actorId)) return;

            String targetLabel = targetType.name().toLowerCase();
            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(ownerId)
                    .actorId(actorId)
                    .type("LIKE")
                    .message("Someone liked your " + targetLabel + ".")
                    .targetId(targetId)
                    .targetType(targetType.name())
                    .deepLinkUrl("/" + targetLabel + "s/" + targetId)
                    .build();

            rabbitTemplate.convertAndSend(exchange, likeRoutingKey, event);
            log.info("Like notification published: actor={} liked {}:{} owned by {}",
                    actorId, targetType, targetId, ownerId);

        } catch (Exception e) {
            // Non-critical: reaction already saved — don't roll back over a missed notification
            log.error("Failed to publish like notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Resolves the content owner's userId for a given target.
     * STORY reactions: no notification (story authors are notified via views, not likes).
     */
    private Long resolveOwnerId(Long targetId, TargetType targetType) {
        return switch (targetType) {
            case POST -> postServiceClient.getPostOwnerId(targetId);
            case COMMENT -> commentServiceClient.getCommentOwnerId(targetId);
            case STORY -> null; // story likes don't trigger notifications
        };
    }

    // ── Rest of the methods unchanged ─────────────────────────────────────

    @Override
    @Transactional
    public void unreact(Long userId, Long targetId, TargetType targetType) {

        Like existing = likeRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
                .orElseThrow(() -> new LikeNotFoundException(
                        "No reaction found for this user on " +
                                targetType.name().toLowerCase() + " " + targetId));

        likeRepository.deleteByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
        decrementTargetCounter(targetId, targetType);

        log.info("Reaction removed: userId={} unreacted from {}:{}", userId, targetType, targetId);
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
        if (oldType == newReactionType) return mapToResponse(existing);

        existing.setReactionType(newReactionType);
        Like updated = likeRepository.save(existing);

        log.info("Reaction changed: userId={} changed {} → {} on {}:{}",
                userId, oldType, newReactionType, targetType, targetId);

        return mapToResponse(updated);
    }

    @Override
    public boolean hasReacted(Long userId, Long targetId, TargetType targetType) {
        return likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
    }

    @Override
    public LikeResponse getUserReaction(Long userId, Long targetId, TargetType targetType) {
        Optional<Like> like = likeRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
        return likeRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
                .map(this::mapToResponse)
                .orElseThrow(() -> new LikeNotFoundException(
                        "No reaction found for user " + userId +
                                " on " + targetType.name().toLowerCase() + " " + targetId));
    }

    @Override
    public Page<LikeResponse> getReactionsByTarget(Long targetId, TargetType targetType, Pageable pageable) {
        return likeRepository
                .findByTargetIdAndTargetTypeOrderByCreatedAtDesc(targetId, targetType, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<LikeResponse> getReactionsByUser(Long userId, Pageable pageable) {
        return likeRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public long getReactionCount(Long targetId, TargetType targetType) {
        return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
    }

    @Override
    public long getReactionCountByType(Long targetId, TargetType targetType, ReactionType reactionType) {
        return likeRepository.countByTargetIdAndTargetTypeAndReactionType(targetId, targetType, reactionType);
    }

    @Override
    public ReactionSummaryResponse getReactionSummary(Long targetId, TargetType targetType) {
        List<Object[]> rawSummary = likeRepository.getReactionSummaryRaw(targetId, targetType);
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

    private void incrementTargetCounter(Long targetId, TargetType targetType) {
        try {
            switch (targetType) {
                case POST -> postServiceClient.incrementLikesCount(targetId);
                case COMMENT -> commentServiceClient.incrementLikesCount(targetId);
                case STORY -> log.debug("Story reactions do not increment a counter service");
            }
        } catch (Exception e) {
            log.warn("Failed to increment likesCount on {}:{} — {}", targetType, targetId, e.getMessage());
        }
    }

    private void decrementTargetCounter(Long targetId, TargetType targetType) {
        try {
            switch (targetType) {
                case POST -> postServiceClient.decrementLikesCount(targetId);
                case COMMENT -> commentServiceClient.decrementLikesCount(targetId);
                case STORY -> log.debug("Story reactions do not decrement a counter service");
            }
        } catch (Exception e) {
            log.warn("Failed to decrement likesCount on {}:{} — {}", targetType, targetId, e.getMessage());
        }
    }

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