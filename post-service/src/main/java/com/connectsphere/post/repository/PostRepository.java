package com.connectsphere.post.repository;

import com.connectsphere.post.entity.Post;
import com.connectsphere.post.enums.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Find non-deleted post by id
    Optional<Post> findByIdAndIsDeletedFalse(Long id);

    // All active posts by a specific author
    Page<Post> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(
            Long authorId, Pageable pageable);

    // Public posts by a specific author (for guest/other user profile view)
    Page<Post> findByAuthorIdAndVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
            Long authorId, PostVisibility visibility, Pageable pageable);

    // All PUBLIC posts — guest feed
    Page<Post> findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
            PostVisibility visibility, Pageable pageable);

    // News feed: posts from a list of followed user IDs + both visibility types
    @Query("SELECT p FROM Post p WHERE p.authorId IN :authorIds " +
            "AND p.visibility IN ('PUBLIC', 'FOLLOWERS_ONLY') " +
            "AND p.isDeleted = false " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findFeedByAuthorIds(
            @Param("authorIds") List<Long> authorIds, Pageable pageable);

    // Full-text search on post content
    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND p.visibility = 'PUBLIC' AND p.isDeleted = false " +
            "ORDER BY p.createdAt DESC")
    Page<Post> searchByContent(@Param("keyword") String keyword, Pageable pageable);

    // Count active posts by author (for profile stats)
    long countByAuthorIdAndIsDeletedFalse(Long authorId);

    // Used by like-service / comment-service counter endpoints
    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId AND p.isDeleted = false")
    int incrementLikesCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.likesCount = GREATEST(p.likesCount - 1, 0) WHERE p.id = :postId AND p.isDeleted = false")
    int decrementLikesCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentsCount = p.commentsCount + 1 WHERE p.id = :postId AND p.isDeleted = false")
    int incrementCommentsCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentsCount = GREATEST(p.commentsCount - 1, 0) WHERE p.id = :postId AND p.isDeleted = false")
    int decrementCommentsCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.sharesCount = p.sharesCount + 1 WHERE p.id = :postId AND p.isDeleted = false")
    int incrementSharesCount(@Param("postId") Long postId);
}