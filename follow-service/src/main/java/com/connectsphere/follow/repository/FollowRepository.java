package com.connectsphere.follow.repository;

import com.connectsphere.follow.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // Find a specific follow relationship
    Optional<Follow> findByFollowerIdAndFolloweeId(
            Long followerId, Long followeeId);

    // Check if a follow relationship exists
    boolean existsByFollowerIdAndFolloweeId(
            Long followerId, Long followeeId);

    // All users who follow a specific user (their followers)
    Page<Follow> findByFolloweeIdOrderByCreatedAtDesc(
            Long followeeId, Pageable pageable);

    // All users a specific user is following
    Page<Follow> findByFollowerIdOrderByCreatedAtDesc(
            Long followerId, Pageable pageable);

    // Raw list of followee IDs — used by post-service feed query
    // Returns only the IDs for efficiency (no full entity needed)
    @Query("SELECT f.followeeId FROM Follow f WHERE f.followerId = :followerId")
    List<Long> findFolloweeIdsByFollowerId(@Param("followerId") Long followerId);

    // Raw list of follower IDs
    @Query("SELECT f.followerId FROM Follow f WHERE f.followeeId = :followeeId")
    List<Long> findFollowerIdsByFolloweeId(@Param("followeeId") Long followeeId);

    // Follower count for a user
    long countByFolloweeId(Long followeeId);

    // Following count for a user
    long countByFollowerId(Long followerId);

    // Mutual follows: users that both A follows B and B follows A
    // Returns followee IDs of A that also follow A back
    @Query("""
           SELECT f1.followeeId FROM Follow f1
           WHERE f1.followerId = :userId
           AND EXISTS (
               SELECT 1 FROM Follow f2
               WHERE f2.followerId = f1.followeeId
               AND f2.followeeId = :userId
           )
           """)
    List<Long> findMutualFollowIds(@Param("userId") Long userId);

    // Delete a follow relationship
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    // Suggested users: people followed by users that userId follows,
    // but not yet followed by userId, and not userId themselves.
    // Second-degree connections (friend-of-friend pattern).
    @Query("""
           SELECT DISTINCT f2.followeeId
           FROM Follow f1
           JOIN Follow f2 ON f1.followeeId = f2.followerId
           WHERE f1.followerId = :userId
           AND f2.followeeId <> :userId
           AND f2.followeeId NOT IN (
               SELECT f3.followeeId FROM Follow f3
               WHERE f3.followerId = :userId
           )
           """)
    List<Long> findSuggestedUserIds(
            @Param("userId") Long userId,
            Pageable pageable);
}