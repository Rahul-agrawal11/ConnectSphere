package com.connectsphere.search.repository;

import com.connectsphere.search.entity.Hashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    // Find a hashtag by its normalised tag string
    Optional<Hashtag> findByTag(String tag);

    // Partial tag search — for autocomplete / search-as-you-type
    @Query("SELECT h FROM Hashtag h WHERE h.tag LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY h.postCount DESC")
    List<Hashtag> searchByTagContaining(
            @Param("query") String query, Pageable pageable);

    // Trending hashtags — ordered by post count descending
    @Query("SELECT h FROM Hashtag h WHERE h.postCount >= :minCount " +
            "ORDER BY h.postCount DESC")
    List<Hashtag> findTrendingHashtags(
            @Param("minCount") int minCount, Pageable pageable);

    // Increment postCount atomically
    @Modifying
    @Query("UPDATE Hashtag h SET h.postCount = h.postCount + 1 " +
            "WHERE h.id = :hashtagId")
    void incrementPostCount(@Param("hashtagId") Long hashtagId);

    // Decrement postCount (floor at 0)
    @Modifying
    @Query("UPDATE Hashtag h SET h.postCount = GREATEST(h.postCount - 1, 0) " +
            "WHERE h.id = :hashtagId")
    void decrementPostCount(@Param("hashtagId") Long hashtagId);
}