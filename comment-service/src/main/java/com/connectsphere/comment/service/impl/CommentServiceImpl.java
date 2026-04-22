package com.connectsphere.comment.service.impl;

import com.connectsphere.comment.client.PostServiceClient;
import com.connectsphere.comment.dto.request.AddCommentRequest;
import com.connectsphere.comment.dto.request.UpdateCommentRequest;
import com.connectsphere.comment.dto.response.CommentResponse;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.exception.CommentNotFoundException;
import com.connectsphere.comment.exception.UnauthorizedActionException;
import com.connectsphere.comment.repository.CommentRepository;
import com.connectsphere.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostServiceClient postServiceClient;

    // ── Core CRUD ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CommentResponse addComment(Long authorId, AddCommentRequest request) {

        // Enforce two-level threading:
        // If parentCommentId is provided, verify the parent is a top-level comment
        if (request.getParentCommentId() != null) {
            Comment parent = findActiveComment(request.getParentCommentId());

            if (parent.getParentCommentId() != null) {
                throw new IllegalArgumentException(
                        "Replies to replies are not allowed. " +
                                "Only two levels of threading are supported.");
            }
            // Ensure the parent belongs to the same post
            if (!parent.getPostId().equals(request.getPostId())) {
                throw new IllegalArgumentException(
                        "Parent comment does not belong to the specified post.");
            }
        }

        Comment comment = Comment.builder()
                .postId(request.getPostId())
                .authorId(authorId)
                .parentCommentId(request.getParentCommentId())
                .content(request.getContent())
                .build();

        Comment saved = commentRepository.save(comment);

        // Notify post-service to increment commentsCount
        // Wrapped in try-catch so a post-service hiccup does not fail the comment
        try {
            postServiceClient.incrementCommentsCount(request.getPostId());
        } catch (Exception e) {
            log.warn("Failed to increment commentsCount on post {}: {}",
                    request.getPostId(), e.getMessage());
        }

        log.info("Comment added: id={} on postId={} by authorId={}",
                saved.getId(), saved.getPostId(), authorId);
        return mapToResponse(saved);
    }

    @Override
    public CommentResponse getCommentById(Long commentId) {
        return mapToResponse(findActiveComment(commentId));
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, Long requesterId,
                                         UpdateCommentRequest request) {
        Comment comment = findActiveComment(commentId);
        enforceOwnership(comment, requesterId);
        comment.setContent(request.getContent());
        return mapToResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long requesterId) {
        Comment comment = findActiveComment(commentId);
        enforceOwnership(comment, requesterId);
        softDelete(comment);

        // Notify post-service to decrement commentsCount
        try {
            postServiceClient.decrementCommentsCount(comment.getPostId());
        } catch (Exception e) {
            log.warn("Failed to decrement commentsCount on post {}: {}",
                    comment.getPostId(), e.getMessage());
        }

        log.info("Comment soft-deleted: id={} by requesterId={}", commentId, requesterId);
    }

    // ── Threaded Retrieval ───────────────────────────────────────────────

    @Override
    public Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable) {
        return commentRepository
                .findTopLevelByPostId(postId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<CommentResponse> getReplies(Long parentCommentId, Pageable pageable) {
        // Verify parent exists
        findActiveComment(parentCommentId);
        return commentRepository
                .findRepliesByParentCommentId(parentCommentId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<CommentResponse> getCommentsByUser(Long authorId, Pageable pageable) {
        return commentRepository
                .findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        authorId, pageable)
                .map(this::mapToResponse);
    }

    // ── Per-Comment Likes ────────────────────────────────────────────────

    @Override
    @Transactional
    public void likeComment(Long commentId) {
        int updated = commentRepository.incrementLikesCount(commentId);
        if (updated == 0) {
            throw new CommentNotFoundException(
                    "Comment not found: " + commentId);
        }
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId) {
        int updated = commentRepository.decrementLikesCount(commentId);
        if (updated == 0) {
            throw new CommentNotFoundException(
                    "Comment not found: " + commentId);
        }
    }

    // ── Counts ───────────────────────────────────────────────────────────

    @Override
    public long getCommentCount(Long postId) {
        return commentRepository.countTopLevelByPostId(postId);
    }

    @Override
    public long getTotalCommentCount(Long postId) {
        return commentRepository.countAllByPostId(postId);
    }

    // ── Admin ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void adminDeleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(
                        "Comment not found: " + commentId));

        softDelete(comment);

        try {
            postServiceClient.decrementCommentsCount(comment.getPostId());
        } catch (Exception e) {
            log.warn("Failed to decrement commentsCount after admin delete: {}",
                    e.getMessage());
        }

        log.info("Comment force-deleted by admin: id={}", commentId);
    }

    // ── Private Helpers ──────────────────────────────────────────────────

    private Comment findActiveComment(Long commentId) {
        return commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new CommentNotFoundException(
                        "Comment not found or has been deleted: " + commentId));
    }

    private void enforceOwnership(Comment comment, Long requesterId) {
        if (!comment.getAuthorId().equals(requesterId)) {
            throw new UnauthorizedActionException(
                    "You are not allowed to modify this comment.");
        }
    }

    /**
     * Soft-delete: mark as deleted and replace content.
     * Thread structure is preserved — "[deleted]" shows in UI
     * so reply context is not broken.
     */
    private void softDelete(Comment comment) {
        comment.setIsDeleted(true);
        comment.setContent("[deleted]");
        commentRepository.save(comment);
    }

    /**
     * Map entity to response DTO.
     */
    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .parentCommentId(comment.getParentCommentId())
                .content(comment.getContent())
                .likesCount(comment.getLikesCount())
                .isReply(comment.getParentCommentId() != null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}