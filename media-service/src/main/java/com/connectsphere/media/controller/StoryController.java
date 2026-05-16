package com.connectsphere.media.controller;

import com.connectsphere.media.dto.response.ApiResponse;
import com.connectsphere.media.dto.response.StoryResponse;
import com.connectsphere.media.dto.response.StoryViewersResponse;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
@Tag(name = "Stories", description = "Ephemeral 24-hour stories")
public class StoryController {

    private final MediaService mediaService;

    @Operation(summary = "Create a new story",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String caption) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Story created",
                        mediaService.createStory(userId, file, caption)));
    }

    @Operation(summary = "Get a story by ID")
    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResponse<StoryResponse>> getStory(
            @PathVariable Long storyId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Story fetched",
                mediaService.getStoryById(storyId, userId)));
    }

    @Operation(summary = "Get active stories for a user")
    @GetMapping("/user/{authorId}")
    public ResponseEntity<ApiResponse<List<StoryResponse>>> getStoriesByUser(
            @PathVariable Long authorId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User stories",
                mediaService.getActiveStoriesByUser(authorId, userId)));
    }

    @Operation(summary = "Get stories feed from followed users",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<StoryResponse>>> getStoriesFeed(
            @RequestParam List<Long> followedUserIds,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Stories feed",
                mediaService.getStoriesFeed(followedUserIds, userId)));
    }

    @Operation(summary = "Mark a story as viewed",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{storyId}/view")
    public ResponseEntity<ApiResponse<StoryResponse>> viewStory(
            @PathVariable Long storyId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Story viewed",
                mediaService.viewStory(storyId, userId)));
    }

    /**
     * GET /api/v1/stories/{storyId}/viewers
     *
     * NEW: Returns the list of users (viewerId + timestamp) who have viewed
     * this story, ordered newest first. Only the story author can call this.
     * Returns 403 for anyone else.
     */
    @Operation(summary = "Get who viewed my story — author only",
            description = "Returns viewer IDs and view timestamps ordered newest first. " +
                    "Returns 403 if caller is not the story author.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{storyId}/viewers")
    public ResponseEntity<ApiResponse<StoryViewersResponse>> getStoryViewers(
            @PathVariable Long storyId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Story viewers",
                mediaService.getStoryViewers(storyId, userId)));
    }

    @Operation(summary = "Delete own story",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{storyId}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(
            @PathVariable Long storyId,
            @RequestHeader("X-User-Id") Long userId) {
        mediaService.deleteStory(storyId, userId);
        return ResponseEntity.ok(ApiResponse.success("Story deleted"));
    }
}