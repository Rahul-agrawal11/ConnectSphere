package com.connectsphere.search.repository;

import com.connectsphere.search.entity.Hashtag;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class HashtagRepositoryTest {

    @Autowired
    private HashtagRepository hashtagRepository;

    @Autowired
    private EntityManager entityManager;

    private Hashtag createHashtag(String tag, int postCount) {
        return Hashtag.builder()
                .tag(tag)
                .postCount(postCount)
                .build();
    }

    @Test
    @DisplayName("Should find hashtag by tag")
    void findByTag_ShouldReturnHashtag() {
        hashtagRepository.save(createHashtag("springboot", 5));

        Optional<Hashtag> found = hashtagRepository.findByTag("springboot");

        assertThat(found).isPresent();
        assertThat(found.get().getPostCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should search hashtags by partial tag")
    void searchByTagContaining_ShouldReturnMatchingTags() {
        hashtagRepository.save(createHashtag("springboot", 5));
        hashtagRepository.save(createHashtag("springsecurity", 3));
        hashtagRepository.save(createHashtag("java", 10));

        List<Hashtag> result =
                hashtagRepository.searchByTagContaining("spring", PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should return trending hashtags by post count")
    void findTrendingHashtags_ShouldReturnTagsAboveMinCount() {
        hashtagRepository.save(createHashtag("java", 10));
        hashtagRepository.save(createHashtag("springboot", 5));
        hashtagRepository.save(createHashtag("unused", 0));

        List<Hashtag> result =
                hashtagRepository.findTrendingHashtags(1, PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPostCount()).isGreaterThanOrEqualTo(result.get(1).getPostCount());
    }

    @Test
    @DisplayName("Should increment post count")
    void incrementPostCount_ShouldIncreaseCount() {
        Hashtag saved = hashtagRepository.saveAndFlush(createHashtag("java", 1));

        hashtagRepository.incrementPostCount(saved.getId());

        entityManager.clear();

        Hashtag found = hashtagRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPostCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should decrement post count")
    void decrementPostCount_ShouldDecreaseCount() {
        Hashtag saved = hashtagRepository.saveAndFlush(createHashtag("java", 2));

        hashtagRepository.decrementPostCount(saved.getId());

        entityManager.clear();

        Hashtag found = hashtagRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPostCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not decrement below zero")
    void decrementPostCount_ShouldNotGoBelowZero() {
        Hashtag saved = hashtagRepository.saveAndFlush(createHashtag("java", 0));

        hashtagRepository.decrementPostCount(saved.getId());

        entityManager.clear();

        Hashtag found = hashtagRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPostCount()).isZero();
    }
}