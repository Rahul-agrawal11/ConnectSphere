package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Story;
import com.connectsphere.media.enums.MediaType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StoryRepositoryTest {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private EntityManager entityManager;

    private Story createStory(Long authorId, boolean active, LocalDateTime expiresAt) {
        return Story.builder()
                .authorId(authorId)
                .mediaUrl("http://localhost:8087/files/stories/story.jpg")
                .caption("Story")
                .mediaType(MediaType.IMAGE)
                .viewsCount(0)
                .expiresAt(expiresAt)
                .isActive(active)
                .build();
    }

    @Test
    @DisplayName("Should find active story by id")
    void findByIdAndIsActiveTrue_ShouldReturnActiveStory() {
        Story saved = storyRepository.save(createStory(10L, true, LocalDateTime.now().plusHours(24)));

        Optional<Story> found = storyRepository.findByIdAndIsActiveTrue(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAuthorId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should not return inactive story")
    void findByIdAndIsActiveTrue_ShouldNotReturnInactiveStory() {
        Story saved = storyRepository.save(createStory(10L, false, LocalDateTime.now().plusHours(24)));

        Optional<Story> found = storyRepository.findByIdAndIsActiveTrue(saved.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find active stories by author")
    void findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc_ShouldReturnStories() {
        storyRepository.save(createStory(10L, true, LocalDateTime.now().plusHours(24)));
        storyRepository.save(createStory(10L, false, LocalDateTime.now().plusHours(24)));
        storyRepository.save(createStory(11L, true, LocalDateTime.now().plusHours(24)));

        List<Story> result = storyRepository.findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(10L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should find active stories by author ids")
    void findActiveStoriesByAuthorIds_ShouldReturnStories() {
        storyRepository.save(createStory(10L, true, LocalDateTime.now().plusHours(24)));
        storyRepository.save(createStory(20L, true, LocalDateTime.now().plusHours(24)));
        storyRepository.save(createStory(30L, true, LocalDateTime.now().plusHours(24)));

        List<Story> result = storyRepository.findActiveStoriesByAuthorIds(List.of(10L, 20L));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should find expired active stories")
    void findExpiredActiveStories_ShouldReturnExpiredActiveStories() {
        storyRepository.save(createStory(10L, true, LocalDateTime.now().minusHours(1)));
        storyRepository.save(createStory(20L, true, LocalDateTime.now().plusHours(1)));

        List<Story> result = storyRepository.findExpiredActiveStories(LocalDateTime.now());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should expire old active stories")
    void expireStories_ShouldMarkExpiredStoriesInactive() {
        Story saved = storyRepository.saveAndFlush(
                createStory(10L, true, LocalDateTime.now().minusHours(1)));

        int updated = storyRepository.expireStories(LocalDateTime.now());

        entityManager.clear();

        assertThat(updated).isEqualTo(1);

        Story found = storyRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should increment story views count")
    void incrementViewsCount_ShouldIncreaseViews() {
        Story saved = storyRepository.saveAndFlush(
                createStory(10L, true, LocalDateTime.now().plusHours(24)));

        int updated = storyRepository.incrementViewsCount(saved.getId());

        entityManager.clear();

        assertThat(updated).isEqualTo(1);

        Story found = storyRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getViewsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should check story belongs to author")
    void existsByIdAndAuthorId_ShouldReturnTrue() {
        Story saved = storyRepository.save(createStory(10L, true, LocalDateTime.now().plusHours(24)));

        boolean exists = storyRepository.existsByIdAndAuthorId(saved.getId(), 10L);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should count active stories by author")
    void countByAuthorIdAndIsActiveTrue_ShouldReturnCount() {
        storyRepository.save(createStory(10L, true, LocalDateTime.now().plusHours(24)));
        storyRepository.save(createStory(10L, false, LocalDateTime.now().plusHours(24)));

        long count = storyRepository.countByAuthorIdAndIsActiveTrue(10L);

        assertThat(count).isEqualTo(1);
    }
}