package com.connectsphere.comment.repository;

import com.connectsphere.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Find a non-deleted comment by id
    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    // Top-level comments for a post (parentCommentId IS NULL), newest first
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId " +
            "AND c.parentCommentId IS NULL " +
            "AND c.isDeleted = false " +
            "ORDER BY c.createdAt ASC")
    Page<Comment> findTopLevelByPostId(
            @Param("postId") Long postId, Pageable pageable);

    // Replies for a parent comment, oldest first (threaded order)
    @Query("SELECT c FROM Comment c WHERE c.parentCommentId = :parentCommentId " +
            "AND c.isDeleted = false " +
            "ORDER BY c.createdAt ASC")
    Page<Comment> findRepliesByParentCommentId(
            @Param("parentCommentId") Long parentCommentId, Pageable pageable);

    // All comments by a user (for profile / moderation view)
    Page<Comment> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(
            Long authorId, Pageable pageable);

    // Count of active top-level comments on a post
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.postId = :postId " +
            "AND c.parentCommentId IS NULL AND c.isDeleted = false")
    long countTopLevelByPostId(@Param("postId") Long postId);

    // Count total comments (top-level + replies) on a post
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.postId = :postId " +
            "AND c.isDeleted = false")
    long countAllByPostId(@Param("postId") Long postId);

    // Increment per-comment likes count
    @Modifying
    @Query("UPDATE Comment c SET c.likesCount = c.likesCount + 1 " +
            "WHERE c.id = :commentId AND c.isDeleted = false")
    int incrementLikesCount(@Param("commentId") Long commentId);

    // Decrement per-comment likes count (floor at 0)
    @Modifying
    @Query("UPDATE Comment c SET c.likesCount = GREATEST(c.likesCount - 1, 0) " +
            "WHERE c.id = :commentId AND c.isDeleted = false")
    int decrementLikesCount(@Param("commentId") Long commentId);
}