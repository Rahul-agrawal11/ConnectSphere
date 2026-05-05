package com.connectsphere.media.controller;

import com.connectsphere.media.dto.response.ApiResponse;
import com.connectsphere.media.dto.response.StoryResponse;
import com.connectsphere.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Story REST controller.
 * Stories are ephemeral media visible for 24 hours.
 *
 * FIX: Public read endpoints (GET /user/{authorId}, GET /feed, GET /{storyId})
 * now use @RequestHeader(value = "X-User-Id", required = false) so that
 * unauthenticated/guest requests don't fail with MissingRequestHeaderException.
 *
 * Write endpoints (POST, DELETE, POST /{id}/view) still require X-User-Id
 * because the gateway always injects it for authenticated routes.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
@Tag(name = "Stories",
        description = "Ephemeral 24-hour stories — create, view, expire")
public class StoryController {

    private final MediaService mediaService;

    // ── Create Story ──────────────────────────────────────────────────────

    @Operation(
            summary = "Create a new story",
            description = "Upload an image or video as a 24-hour story. " +
                    "Send as multipart/form-data with 'file' and " +
                    "optional 'caption' parts.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String caption) {

        StoryResponse response = mediaService.createStory(userId, file, caption);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Story created", response));
    }

    // ── Read ──────────────────────────────────────────────────────────────

    /**
     * Get a story by ID.
     *
     * FIX: userId is now optional (required = false). Guests can view public
     * story metadata. The service uses userId = null when unauthenticated.
     */
    @Operation(summary = "Get a story by ID")
    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResponse<StoryResponse>> getStory(
            @PathVariable Long storyId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success("Story fetched",
                        mediaService.getStoryById(storyId, userId)));
    }

    /**
     * Get all active stories for a specific user.
     *
     * FIX: userId is now optional (required = false). This endpoint is listed
     * as a public route in the gateway (/api/v1/stories/user/**). Without
     * required=false, Spring throws MissingRequestHeaderException (→ 400/500)
     * when no JWT is present and the gateway does not inject X-User-Id.
     */
    @Operation(summary = "Get all active stories for a user")
    @GetMapping("/user/{authorId}")
    public ResponseEntity<ApiResponse<List<StoryResponse>>> getStoriesByUser(
            @PathVariable Long authorId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success("User stories",
                        mediaService.getActiveStoriesByUser(authorId, userId)));
    }

    /**
     * Get stories feed from followed users.
     *
     * FIX: userId is now optional (required = false). Authenticated users
     * get their feed; guests get an empty list (followedUserIds will be empty).
     */
    @Operation(
            summary = "Get stories feed from followed users",
            description = "Pass followedUserIds as a comma-separated list. " +
                    "Typically populated by calling follow-service first.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<StoryResponse>>> getStoriesFeed(
            @RequestParam List<Long> followedUserIds,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success("Stories feed",
                        mediaService.getStoriesFeed(followedUserIds, userId)));
    }

    // ── View Story (increments view count) ───────────────────────────────

    @Operation(
            summary = "View a story (increments view count)",
            description = "Call this endpoint when the user opens a story. " +
                    "Author's own views are not counted.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{storyId}/view")
    public ResponseEntity<ApiResponse<StoryResponse>> viewStory(
            @PathVariable Long storyId,
            @RequestHeader("X-User-Id") Long userId) {

        StoryResponse response = mediaService.viewStory(storyId, userId);
        return ResponseEntity.ok(ApiResponse.success("Story viewed", response));
    }

    // ── Delete Story ──────────────────────────────────────────────────────

    @Operation(
            summary = "Delete own story",
            description = "Marks the story as inactive immediately. " +
                    "The scheduler also expires stories after 24 hours.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{storyId}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(
            @PathVariable Long storyId,
            @RequestHeader("X-User-Id") Long userId) {

        mediaService.deleteStory(storyId, userId);
        return ResponseEntity.ok(ApiResponse.success("Story deleted"));
    }
}