package com.connectsphere.comment.service;

import com.connectsphere.comment.dto.request.AddCommentRequest;
import com.connectsphere.comment.dto.request.UpdateCommentRequest;
import com.connectsphere.comment.dto.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Comment service contract.
 */
public interface CommentService {

    // Core CRUD
    CommentResponse addComment(Long authorId, AddCommentRequest request);
    CommentResponse getCommentById(Long commentId);
    CommentResponse updateComment(Long commentId, Long requesterId,
                                  UpdateCommentRequest request);
    void deleteComment(Long commentId, Long requesterId);

    // Threaded retrieval
    Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable);
    Page<CommentResponse> getReplies(Long parentCommentId, Pageable pageable);
    Page<CommentResponse> getCommentsByUser(Long authorId, Pageable pageable);

    // Per-comment likes
    void likeComment(Long commentId);
    void unlikeComment(Long commentId);

    // Counts
    long getCommentCount(Long postId);
    long getTotalCommentCount(Long postId);

    // Owner lookup — called by like-service to resolve notification recipient
    Long getCommentOwnerId(Long commentId);

    // Admin
    void adminDeleteComment(Long commentId);
}