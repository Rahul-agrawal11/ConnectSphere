package com.connectsphere.like.controller;

import com.connectsphere.like.dto.request.ReactRequest;
import com.connectsphere.like.dto.response.ApiResponse;
import com.connectsphere.like.dto.response.LikeResponse;
import com.connectsphere.like.dto.response.ReactionSummaryResponse;
import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import com.connectsphere.like.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Like/Reaction REST controller.
 *
 * All write endpoints read X-User-Id from the gateway-injected header.
 * Read endpoints (summary, count, hasReacted) are open for guest access.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
@Tag(name = "Likes / Reactions",
        description = "Polymorphic reactions on posts and comments")
public class LikeController {

    private final LikeService likeService;

    // ── React ────────────────────────────────────────────────────────────

    @Operation(
            summary = "React to a post or comment",
            description = "One reaction per user per target. " +
                    "Use PUT /change to update an existing reaction.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<ApiResponse<LikeResponse>> react(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ReactRequest request) {

        LikeResponse response = likeService.react(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reaction saved", response));
    }

    // ── Unreact ──────────────────────────────────────────────────────────

    @Operation(
            summary = "Remove a reaction from a post or comment",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unreact(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long targetId,
            @RequestParam TargetType targetType) {

        likeService.unreact(userId, targetId, targetType);
        return ResponseEntity.ok(ApiResponse.success("Reaction removed"));
    }

    // ── Change Reaction ───────────────────────────────────────────────────

    @Operation(
            summary = "Change an existing reaction type",
            description = "Updates reactionType in-place without double-counting counters. " +
                    "Requires an existing reaction (react first).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/change")
    public ResponseEntity<ApiResponse<LikeResponse>> changeReaction(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long targetId,
            @RequestParam TargetType targetType,
            @RequestParam ReactionType newReactionType) {

        LikeResponse response = likeService.changeReaction(
                userId, targetId, targetType, newReactionType);
        return ResponseEntity.ok(ApiResponse.success("Reaction updated", response));
    }

    // ── Query: Has Reacted ────────────────────────────────────────────────

    @Operation(summary = "Check if the current user has reacted to a target")
    @GetMapping("/has-reacted")
    public ResponseEntity<ApiResponse<Boolean>> hasReacted(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam Long targetId,
            @RequestParam TargetType targetType) {

        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success("Not reacted", false));
        }
        boolean result = likeService.hasReacted(userId, targetId, targetType);
        return ResponseEntity.ok(ApiResponse.success("Has reacted", result));
    }

    // ── Query: Get User's Reaction ────────────────────────────────────────

    @Operation(
            summary = "Get the current user's reaction on a specific target",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/my-reaction")
    public ResponseEntity<ApiResponse<LikeResponse>> getUserReaction(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long targetId,
            @RequestParam TargetType targetType) {

        LikeResponse response = likeService.getUserReaction(
                userId, targetId, targetType);
        return ResponseEntity.ok(ApiResponse.success("User reaction", response));
    }

    // ── Query: All Reactions on a Target ─────────────────────────────────

    @Operation(summary = "Get all reactions on a specific target (paginated)")
    @GetMapping("/target")
    public ResponseEntity<ApiResponse<Page<LikeResponse>>> getReactionsByTarget(
            @RequestParam Long targetId,
            @RequestParam TargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LikeResponse> reactions = likeService.getReactionsByTarget(
                targetId, targetType, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Reactions fetched", reactions));
    }

    // ── Query: All Reactions by a User ────────────────────────────────────

    @Operation(
            summary = "Get all reactions made by the current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/my-reactions")
    public ResponseEntity<ApiResponse<Page<LikeResponse>>> getMyReactions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("My reactions",
                        likeService.getReactionsByUser(userId, pageable)));
    }

    // ── Query: Count ──────────────────────────────────────────────────────

    @Operation(summary = "Get total reaction count for a target")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getReactionCount(
            @RequestParam Long targetId,
            @RequestParam TargetType targetType) {

        return ResponseEntity.ok(
                ApiResponse.success("Reaction count",
                        likeService.getReactionCount(targetId, targetType)));
    }

    @Operation(summary = "Get reaction count for a specific type on a target")
    @GetMapping("/count/by-type")
    public ResponseEntity<ApiResponse<Long>> getReactionCountByType(
            @RequestParam Long targetId,
            @RequestParam TargetType targetType,
            @RequestParam ReactionType reactionType) {

        return ResponseEntity.ok(
                ApiResponse.success("Reaction count by type",
                        likeService.getReactionCountByType(
                                targetId, targetType, reactionType)));
    }

    // ── Query: Reaction Summary ───────────────────────────────────────────

    @Operation(
            summary = "Get reaction summary (emoji bar data) for a target",
            description = "Returns a map of reactionType → count for all reaction " +
                    "types present on the target. Used by the frontend to " +
                    "render the emoji reaction bar."
    )
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReactionSummaryResponse>> getReactionSummary(
            @RequestParam Long targetId,
            @RequestParam TargetType targetType) {

        ReactionSummaryResponse summary = likeService.getReactionSummary(
                targetId, targetType);
        return ResponseEntity.ok(
                ApiResponse.success("Reaction summary", summary));
    }
}