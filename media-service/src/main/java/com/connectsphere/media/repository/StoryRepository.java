package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    // Find an active story by ID
    Optional<Story> findByIdAndIsActiveTrue(Long id);

    // All active stories by a specific author — newest first
    List<Story> findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(
            Long authorId);

    // Active stories from a list of author IDs (for "stories feed")
    @Query("SELECT s FROM Story s WHERE s.authorId IN :authorIds " +
            "AND s.isActive = true " +
            "ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByAuthorIds(
            @Param("authorIds") List<Long> authorIds);

    // Stories that have passed their expiry time and are still active
    // Used by the scheduler to find stories to expire
    @Query("SELECT s FROM Story s WHERE s.expiresAt <= :now " +
            "AND s.isActive = true")
    List<Story> findExpiredActiveStories(@Param("now") LocalDateTime now);

    // Bulk expire: set isActive = false for all stories past expiry
    @Modifying
    @Query("UPDATE Story s SET s.isActive = false " +
            "WHERE s.expiresAt <= :now AND s.isActive = true")
    int expireStories(@Param("now") LocalDateTime now);

    // Atomic view count increment — avoids race conditions
    @Modifying
    @Query("UPDATE Story s SET s.viewsCount = s.viewsCount + 1 " +
            "WHERE s.id = :storyId AND s.isActive = true")
    int incrementViewsCount(@Param("storyId") Long storyId);

    // Check if a story belongs to a specific author
    boolean existsByIdAndAuthorId(Long id, Long authorId);

    // Count active stories for a user
    long countByAuthorIdAndIsActiveTrue(Long authorId);
}