package com.connectsphere.media.repository;

import com.connectsphere.media.entity.StoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, Long> {

    /** Check if a specific user has already viewed this story */
    boolean existsByStoryIdAndViewerId(Long storyId, Long viewerId);

    /** Get all view records for a story — ordered newest first */
    Page<StoryView> findByStoryIdOrderByViewedAtDesc(Long storyId, Pageable pageable);

    /** Get all view records for a story — used to load all viewers */
    List<StoryView> findByStoryIdOrderByViewedAtDesc(Long storyId);

    /** Find a specific user's view record */
    Optional<StoryView> findByStoryIdAndViewerId(Long storyId, Long viewerId);

    /** Total unique viewer count for a story */
    long countByStoryId(Long storyId);

    /** Get the IDs of all users who viewed a story — ordered newest first */
    @Query("SELECT sv.viewerId FROM StoryView sv " +
            "WHERE sv.storyId = :storyId " +
            "ORDER BY sv.viewedAt DESC")
    List<Long> findViewerIdsByStoryId(@Param("storyId") Long storyId);

    /** Paginated viewer IDs */
    @Query("SELECT sv.viewerId FROM StoryView sv " +
            "WHERE sv.storyId = :storyId " +
            "ORDER BY sv.viewedAt DESC")
    Page<Long> findViewerIdsByStoryIdPaged(
            @Param("storyId") Long storyId, Pageable pageable);
}