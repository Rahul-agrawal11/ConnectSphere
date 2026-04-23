package com.connectsphere.search.repository;

import com.connectsphere.search.entity.PostHashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    // All hashtag IDs for a specific post
    @Query("SELECT ph.hashtagId FROM PostHashtag ph WHERE ph.postId = :postId")
    List<Long> findHashtagIdsByPostId(@Param("postId") Long postId);

    // All post IDs for a specific hashtag (paginated) — newest first
    @Query("SELECT ph.postId FROM PostHashtag ph " +
            "WHERE ph.hashtagId = :hashtagId " +
            "ORDER BY ph.createdAt DESC")
    Page<Long> findPostIdsByHashtagId(
            @Param("hashtagId") Long hashtagId, Pageable pageable);

    // Check if a post-hashtag mapping exists
    boolean existsByPostIdAndHashtagId(Long postId, Long hashtagId);

    // Delete all mappings for a post (on post deletion / re-index)
    @Modifying
    @Query("DELETE FROM PostHashtag ph WHERE ph.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    // Count posts using a specific hashtag
    long countByHashtagId(Long hashtagId);
}