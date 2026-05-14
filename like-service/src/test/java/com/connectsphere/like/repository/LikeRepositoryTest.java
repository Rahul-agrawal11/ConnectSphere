package com.connectsphere.like.repository;

import com.connectsphere.like.entity.Like;
import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    private Like createLike(Long userId, Long targetId, TargetType targetType, ReactionType reactionType) {
        return Like.builder()
                .userId(userId)
                .targetId(targetId)
                .targetType(targetType)
                .reactionType(reactionType)
                .build();
    }

    @Test
    @DisplayName("Should find user reaction on target")
    void findByUserIdAndTargetIdAndTargetType_ShouldReturnLike() {
        likeRepository.save(createLike(10L, 100L, TargetType.POST, ReactionType.LIKE));

        Optional<Like> found = likeRepository.findByUserIdAndTargetIdAndTargetType(
                10L, 100L, TargetType.POST);

        assertThat(found).isPresent();
        assertThat(found.get().getReactionType()).isEqualTo(ReactionType.LIKE);
    }

    @Test
    @DisplayName("Should check user already reacted")
    void existsByUserIdAndTargetIdAndTargetType_ShouldReturnTrue() {
        likeRepository.save(createLike(10L, 100L, TargetType.POST, ReactionType.LOVE));

        boolean exists = likeRepository.existsByUserIdAndTargetIdAndTargetType(
                10L, 100L, TargetType.POST);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should find reactions by target")
    void findByTargetIdAndTargetTypeOrderByCreatedAtDesc_ShouldReturnReactions() {
        likeRepository.save(createLike(10L, 100L, TargetType.POST, ReactionType.LIKE));
        likeRepository.save(createLike(11L, 100L, TargetType.POST, ReactionType.LOVE));
        likeRepository.save(createLike(12L, 200L, TargetType.POST, ReactionType.HAHA));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Like> result = likeRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(
                100L, TargetType.POST, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find reactions by user")
    void findByUserIdOrderByCreatedAtDesc_ShouldReturnUserReactions() {
        likeRepository.save(createLike(10L, 100L, TargetType.POST, ReactionType.LIKE));
        likeRepository.save(createLike(10L, 200L, TargetType.COMMENT, ReactionType.LOVE));
        likeRepository.save(createLike(11L, 300L, TargetType.POST, ReactionType.WOW));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Like> result = likeRepository.findByUserIdOrderByCreatedAtDesc(10L, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should count reactions by target")
    void countByTargetIdAndTargetType_ShouldReturnCount() {
        likeRepository.save(createLike(10L, 100L, TargetType.POST, ReactionType.LIKE));
        likeRepository.save(createLike(11L, 100L, TargetType.POST, ReactionType.LOVE));

        long count = likeRepository.countByTargetIdAndTargetType(100L, TargetType.POST);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count reactions by type")
    void countByTargetIdAndTargetTypeAndReactionType_ShouldReturnCount() {
        likeRepository.save(createLike(10L, 100L, TargetType.POST, ReactionType.LIKE));
        likeRepository.save(createLike(11L, 100L, TargetType.POST, ReactionType.LIKE));
        likeRepository.save(createLike(12L, 100L, TargetType.POST, ReactionType.LOVE));

        long count = likeRepository.countByTargetIdAndTargetTypeAndReactionType(
                100L, TargetType.POST, ReactionType.LIKE);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return reaction summary")
    void getReactionSummaryRaw_ShouldReturnGroupedCounts() {
        likeRepository.save(createLike(10L, 100L, TargetType.POST, ReactionType.LIKE));
        likeRepository.save(createLike(11L, 100L, TargetType.POST, ReactionType.LIKE));
        likeRepository.save(createLike(12L, 100L, TargetType.POST, ReactionType.LOVE));

        List<Object[]> summary = likeRepository.getReactionSummaryRaw(100L, TargetType.POST);

        assertThat(summary).hasSize(2);

        long total = summary.stream()
                .mapToLong(row -> (Long) row[1])
                .sum();

        assertThat(total).isEqualTo(3);
    }

    @Test
    @DisplayName("Should delete reaction by user and target")
    void deleteByUserIdAndTargetIdAndTargetType_ShouldDeleteReaction() {
        likeRepository.save(createLike(10L, 100L, TargetType.POST, ReactionType.LIKE));

        likeRepository.deleteByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST);

        Optional<Like> found = likeRepository.findByUserIdAndTargetIdAndTargetType(
                10L, 100L, TargetType.POST);

        assertThat(found).isEmpty();
    }
}