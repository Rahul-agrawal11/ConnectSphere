package com.connectsphere.follow.controller;

import com.connectsphere.follow.dto.response.ApiResponse;
import com.connectsphere.follow.dto.response.FollowCountResponse;
import com.connectsphere.follow.dto.response.FollowResponse;
import com.connectsphere.follow.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Follow REST controller.
 *
 * Write endpoints (follow, unfollow) require X-User-Id.
 * Read endpoints (followers, counts, suggestions) are open.
 *
 * The /internal/following-ids endpoint is called by post-service
 * to build the personalized news feed.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
@Tag(name = "Follows",
        description = "Social graph — follow relationships, counts and suggestions")
public class FollowController {

    private final FollowService followService;

    // ── Follow ───────────────────────────────────────────────────────────

    @Operation(
            summary = "Follow a user",
            description = "Creates a directed follow: requester → targetUserId. " +
                    "Self-follow and duplicate follows are rejected.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{followeeId}")
    public ResponseEntity<ApiResponse<FollowResponse>> follow(
            @RequestHeader("X-User-Id") Long followerId,
            @PathVariable Long followeeId) {

        FollowResponse response = followService.follow(followerId, followeeId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Successfully followed user", response));
    }

    // ── Unfollow ─────────────────────────────────────────────────────────

    @Operation(
            summary = "Unfollow a user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{followeeId}")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @RequestHeader("X-User-Id") Long followerId,
            @PathVariable Long followeeId) {

        followService.unfollow(followerId, followeeId);
        return ResponseEntity.ok(ApiResponse.success("Successfully unfollowed user"));
    }

    // ── Is Following ─────────────────────────────────────────────────────

    @Operation(
            summary = "Check if the current user follows a target user"
    )
    @GetMapping("/is-following/{followeeId}")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @RequestHeader(value = "X-User-Id", required = false) Long followerId,
            @PathVariable Long followeeId) {

        if (followerId == null) {
            return ResponseEntity.ok(ApiResponse.success("Not following", false));
        }
        boolean result = followService.isFollowing(followerId, followeeId);
        return ResponseEntity.ok(ApiResponse.success("Is following", result));
    }

    // ── Followers ─────────────────────────────────────────────────────────

    @Operation(summary = "Get all followers of a user (paginated)")
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<Page<FollowResponse>>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.success("Followers fetched",
                        followService.getFollowers(userId, pageable)));
    }

    // ── Following ─────────────────────────────────────────────────────────

    @Operation(summary = "Get all users that a user is following (paginated)")
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<Page<FollowResponse>>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.success("Following fetched",
                        followService.getFollowing(userId, pageable)));
    }

    // ── Counts ───────────────────────────────────────────────────────────

    @Operation(summary = "Get follower and following counts for a user")
    @GetMapping("/{userId}/counts")
    public ResponseEntity<ApiResponse<FollowCountResponse>> getFollowCounts(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success("Follow counts",
                        followService.getFollowCounts(userId)));
    }

    @Operation(summary = "Get follower count for a user")
    @GetMapping("/{userId}/followers/count")
    public ResponseEntity<ApiResponse<Long>> getFollowerCount(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success("Follower count",
                        followService.getFollowerCount(userId)));
    }

    @Operation(summary = "Get following count for a user")
    @GetMapping("/{userId}/following/count")
    public ResponseEntity<ApiResponse<Long>> getFollowingCount(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success("Following count",
                        followService.getFollowingCount(userId)));
    }

    // ── Mutual Follows ────────────────────────────────────────────────────

    @Operation(
            summary = "Get mutual follow IDs for a user",
            description = "Returns IDs of users that follow each other with " +
                    "the given user. Used for mutual connection badges."
    )
    @GetMapping("/{userId}/mutual")
    public ResponseEntity<ApiResponse<List<Long>>> getMutualFollowIds(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success("Mutual follow IDs",
                        followService.getMutualFollowIds(userId)));
    }

    // ── Suggested Users ───────────────────────────────────────────────────

    @Operation(
            summary = "Get suggested users to follow",
            description = "Returns user IDs of second-degree connections: " +
                    "people followed by users the requester already follows, " +
                    "but not yet followed by the requester.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<Long>>> getSuggestedUsers(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.success("Suggested users",
                        followService.getSuggestedUserIds(userId, limit)));
    }

    // ── Internal Endpoints ────────────────────────────────────────────────

    @Operation(
            summary = "[Internal] Get raw list of followee IDs for a user",
            description = "Called by post-service to build the personalized " +
                    "news feed. Returns a plain list of Long IDs — " +
                    "no pagination, no wrappers — for efficient feed queries."
    )
    @GetMapping("/internal/following-ids/{userId}")
    public ResponseEntity<List<Long>> getFollowingIds(
            @PathVariable Long userId) {

        return ResponseEntity.ok(followService.getFollowingIds(userId));
    }

    @Operation(
            summary = "[Internal] Get raw list of follower IDs for a user",
            description = "Used by notification-service and other services " +
                    "that need to know who follows a given user."
    )
    @GetMapping("/internal/follower-ids/{userId}")
    public ResponseEntity<List<Long>> getFollowerIds(
            @PathVariable Long userId) {

        return ResponseEntity.ok(followService.getFollowerIds(userId));
    }
}