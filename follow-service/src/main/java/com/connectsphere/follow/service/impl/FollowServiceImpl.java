package com.connectsphere.follow.service.impl;

import com.connectsphere.follow.dto.response.FollowCountResponse;
import com.connectsphere.follow.dto.response.FollowResponse;
import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.event.NotificationEvent;
import com.connectsphere.follow.exception.DuplicateFollowException;
import com.connectsphere.follow.exception.FollowNotFoundException;
import com.connectsphere.follow.exception.SelfFollowException;
import com.connectsphere.follow.repository.FollowRepository;
import com.connectsphere.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-keys.follow}")
    private String followRoutingKey;

    // ── Core Follow Operations ───────────────────────────────────────────

    @Override
    @Transactional
    public FollowResponse follow(Long followerId, Long followeeId) {

        // Prevent self-follow
        if (followerId.equals(followeeId)) {
            throw new SelfFollowException("You cannot follow yourself.");
        }

        // Prevent duplicate follows
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new DuplicateFollowException(
                    "You are already following user " + followeeId + ".");
        }

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .build();

        Follow saved = followRepository.save(follow);

        log.info("Follow created: userId={} now follows userId={}", followerId, followeeId);

        // ── Publish notification event ──────────────────────────────────
        publishFollowNotification(followerId, followeeId);

        return mapToResponse(saved);
    }

    // ── Notification Publisher ───────────────────────────────────────────

    private void publishFollowNotification(Long followerId, Long followeeId) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(followeeId)   // the person being followed gets notified
                    .actorId(followerId)        // the person who clicked Follow
                    .type("FOLLOW")
                    .message("Someone started following you.")
                    .targetId(followerId)
                    .targetType("USER")
                    .deepLinkUrl("/users/" + followerId)
                    .build();

            rabbitTemplate.convertAndSend(exchange, followRoutingKey, event);
            log.info("✅ Follow notification event published: followerId={} → followeeId={}",
                    followerId, followeeId);

        } catch (Exception e) {
            // Non-critical: follow already succeeded — don't roll it back
            // just because the notification broker is temporarily unavailable
            log.error("❌ Failed to publish follow notification event: {}", e.getMessage(), e);
        }
    }

    // ── Rest of the methods unchanged ────────────────────────────────────

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followeeId) {

        if (!followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new FollowNotFoundException(
                    "You are not following user " + followeeId + ".");
        }

        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);

        log.info("Follow removed: userId={} unfollowed userId={}", followerId, followeeId);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Override
    public Page<FollowResponse> getFollowers(Long userId, Pageable pageable) {
        return followRepository
                .findByFolloweeIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<FollowResponse> getFollowing(Long userId, Pageable pageable) {
        return followRepository
                .findByFollowerIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<Long> getFollowingIds(Long userId) {
        return followRepository.findFolloweeIdsByFollowerId(userId);
    }

    @Override
    public List<Long> getFollowerIds(Long userId) {
        return followRepository.findFollowerIdsByFolloweeId(userId);
    }

    @Override
    public FollowCountResponse getFollowCounts(Long userId) {
        return FollowCountResponse.builder()
                .userId(userId)
                .followerCount(followRepository.countByFolloweeId(userId))
                .followingCount(followRepository.countByFollowerId(userId))
                .build();
    }

    @Override
    public long getFollowerCount(Long userId) {
        return followRepository.countByFolloweeId(userId);
    }

    @Override
    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    @Override
    public List<Long> getMutualFollowIds(Long userId) {
        return followRepository.findMutualFollowIds(userId);
    }

    @Override
    public List<Long> getSuggestedUserIds(Long userId, int limit) {
        int cappedLimit = Math.min(limit, 20);
        Pageable pageable = PageRequest.of(0, cappedLimit);
        return followRepository.findSuggestedUserIds(userId, pageable);
    }

    private FollowResponse mapToResponse(Follow follow) {
        return FollowResponse.builder()
                .id(follow.getId())
                .followerId(follow.getFollowerId())
                .followeeId(follow.getFolloweeId())
                .createdAt(follow.getCreatedAt())
                .build();
    }
}