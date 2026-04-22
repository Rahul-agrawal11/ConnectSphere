package com.connectsphere.post.controller;

import com.connectsphere.post.dto.request.CreatePostRequest;
import com.connectsphere.post.dto.request.UpdatePostRequest;
import com.connectsphere.post.dto.response.ApiResponse;
import com.connectsphere.post.dto.response.PostResponse;
import com.connectsphere.post.enums.PostVisibility;
import com.connectsphere.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * Post REST controller.
 *
 * All protected endpoints read X-User-Id from the gateway-injected header.
 * The controller does NOT validate JWTs — that is the gateway's job.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post CRUD, feeds, visibility and counter endpoints")
public class PostController {

    private final PostService postService;

    // ── Create ──────────────────────────────────────────────────────────

    @Operation(summary = "Create a new post",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreatePostRequest request) {

        PostResponse post = postService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created", post));
    }

    // ── Read ────────────────────────────────────────────────────────────

    @Operation(summary = "Get a post by ID")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        // userId may be null for guests accessing PUBLIC posts
        PostResponse post = postService.getPostById(postId,
                userId != null ? userId : -1L);
        return ResponseEntity.ok(ApiResponse.success("Post fetched", post));
    }

    @Operation(summary = "Get posts by a specific user")
    @GetMapping("/user/{authorId}")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPostsByUser(
            @PathVariable Long authorId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        Page<PostResponse> posts = postService.getPostsByUser(
                authorId,
                requesterId != null ? requesterId : -1L,
                pageable);
        return ResponseEntity.ok(ApiResponse.success("Posts fetched", posts));
    }

    @Operation(summary = "Get public post feed (no auth required)")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPublicFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.success("Public feed",
                        postService.getPublicFeed(pageable)));
    }

    @Operation(summary = "Get personalized news feed for authenticated user",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getFeed(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam List<Long> followedUserIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.success("Feed fetched",
                        postService.getFeedForUser(followedUserIds, pageable)));
    }

    @Operation(summary = "Search posts by keyword")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.success("Search results",
                        postService.searchPosts(keyword, pageable)));
    }

    @Operation(summary = "Get post count for a user")
    @GetMapping("/count/{authorId}")
    public ResponseEntity<ApiResponse<Long>> getPostCount(
            @PathVariable Long authorId) {
        return ResponseEntity.ok(
                ApiResponse.success("Post count",
                        postService.getPostCount(authorId)));
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Operation(summary = "Update a post",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdatePostRequest request) {

        PostResponse updated = postService.updatePost(postId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Post updated", updated));
    }

    @Operation(summary = "Change post visibility",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{postId}/visibility")
    public ResponseEntity<ApiResponse<PostResponse>> changeVisibility(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam PostVisibility visibility) {

        PostResponse updated = postService.changeVisibility(postId, userId, visibility);
        return ResponseEntity.ok(ApiResponse.success("Visibility updated", updated));
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Operation(summary = "Delete own post",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Long userId) {

        postService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("Post deleted"));
    }

    @Operation(summary = "[ADMIN] Force delete any post")
    @DeleteMapping("/admin/{postId}")
    public ResponseEntity<ApiResponse<Void>> adminDeletePost(
            @PathVariable Long postId,
            @RequestHeader("X-User-Role") String role) {

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }
        postService.adminDeletePost(postId);
        return ResponseEntity.ok(ApiResponse.success("Post removed by admin"));
    }

    // ── Counter Endpoints (called by like-service / comment-service) ─────

    @Operation(summary = "[Internal] Increment post likes count")
    @PostMapping("/{postId}/likes/increment")
    public ResponseEntity<Void> incrementLikes(@PathVariable Long postId) {
        postService.incrementLikes(postId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "[Internal] Decrement post likes count")
    @PostMapping("/{postId}/likes/decrement")
    public ResponseEntity<Void> decrementLikes(@PathVariable Long postId) {
        postService.decrementLikes(postId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "[Internal] Increment post comments count")
    @PostMapping("/{postId}/comments/increment")
    public ResponseEntity<Void> incrementComments(@PathVariable Long postId) {
        postService.incrementComments(postId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "[Internal] Decrement post comments count")
    @PostMapping("/{postId}/comments/decrement")
    public ResponseEntity<Void> decrementComments(@PathVariable Long postId) {
        postService.decrementComments(postId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "[Internal] Increment post shares count")
    @PostMapping("/{postId}/shares/increment")
    public ResponseEntity<Void> incrementShares(@PathVariable Long postId) {
        postService.incrementShares(postId);
        return ResponseEntity.ok().build();
    }
}