package com.connectsphere.comment.controller;

import com.connectsphere.comment.dto.request.AddCommentRequest;
import com.connectsphere.comment.dto.request.UpdateCommentRequest;
import com.connectsphere.comment.dto.response.ApiResponse;
import com.connectsphere.comment.dto.response.CommentResponse;
import com.connectsphere.comment.service.CommentService;
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

/**
 * Comment REST controller.
 * All write endpoints require X-User-Id (injected by gateway).
 * Read endpoints are open.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Threaded comments and replies on posts")
public class CommentController {

    private final CommentService commentService;

    // ── Add Comment / Reply ──────────────────────────────────────────────

    @Operation(
            summary = "Add a comment or reply to a post",
            description = "Set parentCommentId to null for top-level comments, " +
                    "or to an existing comment ID for replies.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddCommentRequest request) {

        CommentResponse comment = commentService.addComment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added", comment));
    }

    // ── Read ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get a single comment by ID")
    @GetMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> getComment(
            @PathVariable Long commentId) {

        return ResponseEntity.ok(
                ApiResponse.success("Comment fetched",
                        commentService.getCommentById(commentId)));
    }

    @Operation(summary = "Get top-level comments for a post (paginated)")
    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").ascending());
        return ResponseEntity.ok(
                ApiResponse.success("Comments fetched",
                        commentService.getCommentsByPost(postId, pageable)));
    }

    @Operation(summary = "Get replies for a comment (paginated)")
    @GetMapping("/{parentCommentId}/replies")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getReplies(
            @PathVariable Long parentCommentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").ascending());
        return ResponseEntity.ok(
                ApiResponse.success("Replies fetched",
                        commentService.getReplies(parentCommentId, pageable)));
    }

    @Operation(summary = "Get all comments by a specific user",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/user/{authorId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByUser(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.success("User comments fetched",
                        commentService.getCommentsByUser(authorId, pageable)));
    }

    @Operation(summary = "Get top-level comment count for a post")
    @GetMapping("/post/{postId}/count")
    public ResponseEntity<ApiResponse<Long>> getCommentCount(
            @PathVariable Long postId) {

        return ResponseEntity.ok(
                ApiResponse.success("Comment count",
                        commentService.getCommentCount(postId)));
    }

    @Operation(summary = "Get total comment count (including replies) for a post")
    @GetMapping("/post/{postId}/count/total")
    public ResponseEntity<ApiResponse<Long>> getTotalCommentCount(
            @PathVariable Long postId) {

        return ResponseEntity.ok(
                ApiResponse.success("Total comment count",
                        commentService.getTotalCommentCount(postId)));
    }

    // ── Update ───────────────────────────────────────────────────────────

    @Operation(summary = "Update own comment",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateCommentRequest request) {

        CommentResponse updated = commentService.updateComment(
                commentId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Comment updated", updated));
    }

    // ── Delete ───────────────────────────────────────────────────────────

    @Operation(summary = "Delete own comment (soft delete)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("X-User-Id") Long userId) {

        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted"));
    }

    @Operation(summary = "[ADMIN] Force delete any comment")
    @DeleteMapping("/admin/{commentId}")
    public ResponseEntity<ApiResponse<Void>> adminDeleteComment(
            @PathVariable Long commentId,
            @RequestHeader("X-User-Role") String role) {

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }
        commentService.adminDeleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment removed by admin"));
    }

    // ── Per-Comment Likes ─────────────────────────────────────────────────

    @Operation(
            summary = "Like a comment (increments counter only)",
            description = "This endpoint only increments the comment's likesCount. " +
                    "The like-service stores the actual like record and calls this endpoint.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{commentId}/likes/increment")
    public ResponseEntity<Void> likeComment(@PathVariable Long commentId) {
        commentService.likeComment(commentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Unlike a comment (decrements counter only)")
    @PostMapping("/{commentId}/likes/decrement")
    public ResponseEntity<Void> unlikeComment(@PathVariable Long commentId) {
        commentService.unlikeComment(commentId);
        return ResponseEntity.ok().build();
    }
}