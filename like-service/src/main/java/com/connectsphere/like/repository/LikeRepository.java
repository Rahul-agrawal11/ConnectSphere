package com.connectsphere.like.repository;

import com.connectsphere.like.entity.Like;
import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // Find a specific user's reaction on a specific target
    Optional<Like> findByUserIdAndTargetIdAndTargetType(
            Long userId, Long targetId, TargetType targetType);

    // Check if a user has already reacted to a target
    boolean existsByUserIdAndTargetIdAndTargetType(
            Long userId, Long targetId, TargetType targetType);

    // All reactions on a target (paginated)
    Page<Like> findByTargetIdAndTargetTypeOrderByCreatedAtDesc(
            Long targetId, TargetType targetType, Pageable pageable);

    // All reactions by a user (paginated)
    Page<Like> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // All reactions by a user on a specific target type (e.g. all post reactions)
    Page<Like> findByUserIdAndTargetTypeOrderByCreatedAtDesc(
            Long userId, TargetType targetType, Pageable pageable);

    // Total reaction count for a target
    long countByTargetIdAndTargetType(Long targetId, TargetType targetType);

    // Reaction count for a specific type on a target
    long countByTargetIdAndTargetTypeAndReactionType(
            Long targetId, TargetType targetType, ReactionType reactionType);

    // Reaction summary: count grouped by reactionType for a target
    // Returns List of Object[] where [0] = ReactionType, [1] = count
    @Query("SELECT l.reactionType, COUNT(l) FROM Like l " +
            "WHERE l.targetId = :targetId AND l.targetType = :targetType " +
            "GROUP BY l.reactionType")
    List<Object[]> getReactionSummaryRaw(
            @Param("targetId") Long targetId,
            @Param("targetType") TargetType targetType);

    // Delete a specific user's reaction on a target
    void deleteByUserIdAndTargetIdAndTargetType(
            Long userId, Long targetId, TargetType targetType);
}