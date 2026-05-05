package com.connectsphere.follow.repository;

import com.connectsphere.follow.entity.Follow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FollowRepositoryTest {

    @Autowired
    private FollowRepository followRepository;

    private Follow createFollow(Long followerId, Long followeeId) {
        return Follow.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .build();
    }

    @Test
    @DisplayName("Should find follow relationship")
    void findByFollowerIdAndFolloweeId_ShouldReturnFollow() {
        followRepository.save(createFollow(10L, 20L));

        Optional<Follow> found = followRepository.findByFollowerIdAndFolloweeId(10L, 20L);

        assertThat(found).isPresent();
        assertThat(found.get().getFollowerId()).isEqualTo(10L);
        assertThat(found.get().getFolloweeId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Should check follow exists")
    void existsByFollowerIdAndFolloweeId_ShouldReturnTrue() {
        followRepository.save(createFollow(10L, 20L));

        boolean exists = followRepository.existsByFollowerIdAndFolloweeId(10L, 20L);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should find followers of user")
    void findByFolloweeIdOrderByCreatedAtDesc_ShouldReturnFollowers() {
        followRepository.save(createFollow(10L, 20L));
        followRepository.save(createFollow(11L, 20L));
        followRepository.save(createFollow(12L, 30L));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Follow> result = followRepository.findByFolloweeIdOrderByCreatedAtDesc(20L, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find users followed by user")
    void findByFollowerIdOrderByCreatedAtDesc_ShouldReturnFollowing() {
        followRepository.save(createFollow(10L, 20L));
        followRepository.save(createFollow(10L, 30L));
        followRepository.save(createFollow(11L, 40L));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Follow> result = followRepository.findByFollowerIdOrderByCreatedAtDesc(10L, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should return followee ids by follower")
    void findFolloweeIdsByFollowerId_ShouldReturnIds() {
        followRepository.save(createFollow(10L, 20L));
        followRepository.save(createFollow(10L, 30L));

        List<Long> ids = followRepository.findFolloweeIdsByFollowerId(10L);

        assertThat(ids).containsExactlyInAnyOrder(20L, 30L);
    }

    @Test
    @DisplayName("Should return follower ids by followee")
    void findFollowerIdsByFolloweeId_ShouldReturnIds() {
        followRepository.save(createFollow(10L, 20L));
        followRepository.save(createFollow(11L, 20L));

        List<Long> ids = followRepository.findFollowerIdsByFolloweeId(20L);

        assertThat(ids).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    @DisplayName("Should count followers")
    void countByFolloweeId_ShouldReturnCount() {
        followRepository.save(createFollow(10L, 20L));
        followRepository.save(createFollow(11L, 20L));

        long count = followRepository.countByFolloweeId(20L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count following")
    void countByFollowerId_ShouldReturnCount() {
        followRepository.save(createFollow(10L, 20L));
        followRepository.save(createFollow(10L, 30L));

        long count = followRepository.countByFollowerId(10L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find mutual follow ids")
    void findMutualFollowIds_ShouldReturnMutualIds() {
        followRepository.save(createFollow(10L, 20L));
        followRepository.save(createFollow(20L, 10L));
        followRepository.save(createFollow(10L, 30L));

        List<Long> mutualIds = followRepository.findMutualFollowIds(10L);

        assertThat(mutualIds).containsExactly(20L);
    }

    @Test
    @DisplayName("Should find suggested user ids")
    void findSuggestedUserIds_ShouldReturnSecondDegreeConnections() {
        followRepository.save(createFollow(10L, 20L));
        followRepository.save(createFollow(20L, 30L));
        followRepository.save(createFollow(20L, 40L));
        followRepository.save(createFollow(10L, 50L));

        Pageable pageable = PageRequest.of(0, 10);

        List<Long> suggestions = followRepository.findSuggestedUserIds(10L, pageable);

        assertThat(suggestions).containsExactlyInAnyOrder(30L, 40L);
    }

    @Test
    @DisplayName("Should delete follow relationship")
    void deleteByFollowerIdAndFolloweeId_ShouldDeleteFollow() {
        followRepository.save(createFollow(10L, 20L));

        followRepository.deleteByFollowerIdAndFolloweeId(10L, 20L);

        Optional<Follow> found = followRepository.findByFollowerIdAndFolloweeId(10L, 20L);

        assertThat(found).isEmpty();
    }
}