package com.connectsphere.follow.service.impl;

import com.connectsphere.follow.dto.response.FollowCountResponse;
import com.connectsphere.follow.dto.response.FollowResponse;
import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.exception.DuplicateFollowException;
import com.connectsphere.follow.exception.FollowNotFoundException;
import com.connectsphere.follow.exception.SelfFollowException;
import com.connectsphere.follow.repository.FollowRepository;
import com.connectsphere.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ── Core Follow Operations ───────────────────────────────────────────

    @Override
    @Transactional
    public FollowResponse follow(Long followerId, Long followeeId) {

        // Prevent self-follow
        if (followerId.equals(followeeId)) {
            throw new SelfFollowException(
                    "You cannot follow yourself.");
        }

        // Prevent duplicate follows
        if (followRepository.existsByFollowerIdAndFolloweeId(
                followerId, followeeId)) {
            throw new DuplicateFollowException(
                    "You are already following user " + followeeId + ".");
        }

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .build();

        Follow saved = followRepository.save(follow);

        log.info("Follow created: userId={} now follows userId={}",
                followerId, followeeId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followeeId) {

        if (!followRepository.existsByFollowerIdAndFolloweeId(
                followerId, followeeId)) {
            throw new FollowNotFoundException(
                    "You are not following user " + followeeId + ".");
        }

        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);

        log.info("Follow removed: userId={} unfollowed userId={}",
                followerId, followeeId);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(
                followerId, followeeId);
    }

    // ── Social Graph Queries ─────────────────────────────────────────────

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

    // ── Raw ID Lists ─────────────────────────────────────────────────────

    @Override
    public List<Long> getFollowingIds(Long userId) {
        return followRepository.findFolloweeIdsByFollowerId(userId);
    }

    @Override
    public List<Long> getFollowerIds(Long userId) {
        return followRepository.findFollowerIdsByFolloweeId(userId);
    }

    // ── Counts ───────────────────────────────────────────────────────────

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

    // ── Graph Analysis ───────────────────────────────────────────────────

    @Override
    public List<Long> getMutualFollowIds(Long userId) {
        return followRepository.findMutualFollowIds(userId);
    }

    @Override
    public List<Long> getSuggestedUserIds(Long userId, int limit) {
        // Cap suggestions at 20 to avoid large result sets
        int cappedLimit = Math.min(limit, 20);
        Pageable pageable = PageRequest.of(0, cappedLimit);
        return followRepository.findSuggestedUserIds(userId, pageable);
    }

    // ── Private Helpers ──────────────────────────────────────────────────

    private FollowResponse mapToResponse(Follow follow) {
        return FollowResponse.builder()
                .id(follow.getId())
                .followerId(follow.getFollowerId())
                .followeeId(follow.getFolloweeId())
                .createdAt(follow.getCreatedAt())
                .build();
    }
}